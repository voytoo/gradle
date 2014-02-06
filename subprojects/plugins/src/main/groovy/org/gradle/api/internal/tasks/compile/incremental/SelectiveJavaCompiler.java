package org.gradle.api.internal.tasks.compile.incremental;

import org.gradle.api.internal.tasks.compile.Compiler;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.WorkResult;
import org.gradle.util.Clock;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * by Szczepan Faber, created at: 1/16/14
 */
public class SelectiveJavaCompiler implements Compiler<JavaCompileSpec> {
    private Compiler<JavaCompileSpec> compiler;
    private List<File> staleClasses = new LinkedList<File>();
    private final static Logger LOG = Logging.getLogger(SelectiveJavaCompiler.class);
    private List<String> changedSources = new LinkedList<String>();

    public SelectiveJavaCompiler(Compiler<JavaCompileSpec> compiler) {
        this.compiler = compiler;
    }

    public WorkResult execute(JavaCompileSpec spec) {
        Clock clock = new Clock();
        for (File file : staleClasses) {
            file.delete();
        }
        LOG.lifecycle("Deleting {} stale classes took {}", staleClasses.size(), clock.getTime());
        return compiler.execute(spec);
    }

    public void addStaleClass(JavaSourceClass source) {
        staleClasses.add(source.getOutputFile());
        changedSources.add(source.getClassName());
    }

    public List<File> getStaleClasses() {
        return staleClasses;
    }

    public List<String> getChangedSources() {
        return changedSources;
    }
}
