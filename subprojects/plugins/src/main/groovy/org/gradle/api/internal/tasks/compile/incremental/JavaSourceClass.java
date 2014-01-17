package org.gradle.api.internal.tasks.compile.incremental;

import java.io.File;

/**
 * by Szczepan Faber, created at: 1/17/14
 */
public class JavaSourceClass {
    private final String relativePath;
    private final File compileDestination;

    public JavaSourceClass(String relativePath, File compileDestination) {
        this.relativePath = relativePath;
        this.compileDestination = compileDestination;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getClassName() {
        return relativePath.replaceAll("/", ".").replaceAll("\\.java$", "");
    }

    public File getOutputFile() {
        return new File(compileDestination, relativePath.replaceAll("\\.java$", ".class"));
    }
}
