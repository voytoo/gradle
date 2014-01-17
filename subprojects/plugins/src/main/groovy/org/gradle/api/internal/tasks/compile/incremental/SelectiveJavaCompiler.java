package org.gradle.api.internal.tasks.compile.incremental;

import org.gradle.api.internal.tasks.compile.Compiler;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.tasks.WorkResult;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * by Szczepan Faber, created at: 1/16/14
 */
public class SelectiveJavaCompiler implements Compiler<JavaCompileSpec> {
    private Compiler<JavaCompileSpec> compiler;
    private List<File> deleteMe = new LinkedList<File>();

    public SelectiveJavaCompiler(Compiler<JavaCompileSpec> compiler) {
        this.compiler = compiler;
    }

    public WorkResult execute(JavaCompileSpec spec) {
        for (File file : deleteMe) {
            file.delete();
        }
        return compiler.execute(spec);
    }

    public void ensureRefreshed(File file) {
        deleteMe.add(file);
    }
}
