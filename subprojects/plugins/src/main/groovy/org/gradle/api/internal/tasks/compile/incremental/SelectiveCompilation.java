package org.gradle.api.internal.tasks.compile.incremental;

import org.gradle.api.Action;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.util.Clock;

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
    private static final Logger LOG = Logging.getLogger(SelectiveCompilation.class);

    public SelectiveCompilation(IncrementalTaskInputs inputs, FileTree source, FileCollection compileClasspath, final File compileDestination,
                                File classTreeCache, final SelectiveJavaCompiler compiler, final Set<File> sourceDirs) {
        this.compileDestination = compileDestination;
        this.classTreeCache = classTreeCache;
        if (inputs.isIncremental()) {
            Clock clock = new Clock();
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
                        compiler.addStaleClass(source.getOutputFile());
                        changedSourceOnly.include(source.getRelativePath());
                        Iterable<String> dependents = tree.getActualDependents(source.getClassName());
                        for (String d : dependents) {
                            compiler.addStaleClass(source.getOutputFile());
                            changedSourceOnly.include(mapper.toJavaSourceClass(d).getRelativePath());
                        }
                    }
                }
            });
            inputs.removed(new Action<InputFileDetails>() {
                public void execute(InputFileDetails inputFileDetails) {
                    compiler.addStaleClass(mapper.toJavaSourceClass(inputFileDetails.getFile()).getOutputFile());
                }
            });
            //since we're compiling selectively we need to include the classes compiled previously
            this.classpath = compileClasspath.plus(new SimpleFileCollection(compileDestination));
            LOG.lifecycle("Stale classes detection completed in {}. {} class files need recompilation. Full list: {}", clock.getTime(), compiler.getStaleClasses().size(), compiler.getStaleClasses());
        } else {
            this.source = source;
            this.classpath = compileClasspath;
        }
    }

    public void compilationComplete() {
        Clock clock = new Clock();
        ClassDependencyTree tree = new ClassDependencyTree(compileDestination);
        String time1 = clock.getTime();
        tree.writeTo(classTreeCache);
        LOG.lifecycle("Bytecode analysis for incremental java compilation took {} (with serialization: {}). Wrote the class tree into {}.", time1, clock.getTime(), classTreeCache);
    }

    public FileCollection getSource() {
        return source;
    }

    public Iterable<File> getClasspath() {
        return classpath;
    }
}
