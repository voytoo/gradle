package org.gradle.api.internal.tasks.compile.incremental;

import org.gradle.api.Action;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.gradle.api.tasks.util.PatternSet;

import java.io.File;
import java.util.Set;

/**
 * by Szczepan Faber, created at: 1/16/14
 */
public class SelectiveCompilation {
    private final FileCollection source;
    private final FileCollection classpath;
    private final File compileDestination;
    private final File classTreeCache;

    public SelectiveCompilation(IncrementalTaskInputs inputs, FileTree source, FileCollection compileClasspath, final File compileDestination,
                                File classTreeCache, final SelectiveJavaCompiler compiler, final Set<File> sourceDirs) {
        this.compileDestination = compileDestination;
        this.classTreeCache = classTreeCache;
        if (inputs.isIncremental()) {
            final InputOutputMapper mapper = new InputOutputMapper(sourceDirs, compileDestination);

            //load dependency tree
            final ClassDependencyTree tree = ClassDependencyTree.loadFrom(classTreeCache);

            //including only source java classes that were changed
            final PatternSet changedSourceOnly = new PatternSet();
            this.source = source.matching(changedSourceOnly);
            inputs.outOfDate(new Action<InputFileDetails>() {
                public void execute(InputFileDetails inputFileDetails) {
                    String name = inputFileDetails.getFile().getName();
                    if (name.endsWith(".java")) {
                        JavaSourceClass source = mapper.toJavaSourceClass(inputFileDetails.getFile());
                        compiler.ensureRefreshed(source.getOutputFile());
                        changedSourceOnly.include(source.getRelativePath());
                        Iterable<String> dependents = tree.getActualDependents(source.getClassName());
                        for (String d : dependents) {
                            compiler.ensureRefreshed(source.getOutputFile());
                            changedSourceOnly.include(mapper.toJavaSourceClass(d).getRelativePath());
                        }
                    }
                }
            });
            inputs.removed(new Action<InputFileDetails>() {
                public void execute(InputFileDetails inputFileDetails) {
                    compiler.ensureRefreshed(mapper.toJavaSourceClass(inputFileDetails.getFile()).getOutputFile());
                }
            });
            //since we're compiling selectively we need to include the classes compiled previously
            this.classpath = compileClasspath.plus(new SimpleFileCollection(compileDestination));
        } else {
            this.source = source;
            this.classpath = compileClasspath;
        }
    }

    public void compilationComplete() {
        ClassDependencyTree tree = new ClassDependencyTree(compileDestination);
        tree.writeTo(classTreeCache);
    }

    public FileCollection getSource() {
        return source;
    }

    public Iterable<File> getClasspath() {
        return classpath;
    }
}
