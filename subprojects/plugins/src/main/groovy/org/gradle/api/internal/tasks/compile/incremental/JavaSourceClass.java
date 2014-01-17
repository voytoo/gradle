package org.gradle.api.internal.tasks.compile.incremental;

/**
 * by Szczepan Faber, created at: 1/17/14
 */
public class JavaSourceClass {
    private String relativePath;

    public JavaSourceClass(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getClassName() {
        return relativePath.replaceAll("/", ".").replaceAll("\\.java$", "");
    }
}
