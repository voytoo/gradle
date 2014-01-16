package org.gradle.api.internal.tasks.compile.incremental;

import org.gradle.api.Action;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.gradle.api.tasks.util.PatternSet;

import java.io.File;

/**
 * by Szczepan Faber, created at: 1/16/14
 */
public class SelectiveCompilation {
    private final FileCollection source;
    private final FileCollection classpath;
    private final File compileDestination;
    private final File classTreeCache;

    public SelectiveCompilation(IncrementalTaskInputs inputs, FileTree source, FileCollection compileClasspath, File compileDestination, File classTreeCache) {
        this.compileDestination = compileDestination;
        this.classTreeCache = classTreeCache;
        if (inputs.isIncremental()) {
            //load dependency tree
            final ClassDependencyTree tree = ClassDependencyTree.loadFrom(classTreeCache);

            //including only source java classes that were changed
            final PatternSet changedSourceOnly = new PatternSet();
            this.source = source.matching(changedSourceOnly);
            inputs.outOfDate(new Action<InputFileDetails>() {
                public void execute(InputFileDetails inputFileDetails) {
                    String name = inputFileDetails.getFile().getName();
                    if (name.endsWith(".java")) {
                        //hacky, below works only when classe are not in packages
                        changedSourceOnly.include(name);
                        Iterable<String> dependents = tree.getDependents(inputFileDetails.getFile().getName().replaceAll(".java", ""));
                        for (String d : dependents) {
                            changedSourceOnly.include(d + ".java");
                        }
                    }
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
