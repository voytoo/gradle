package org.gradle.api.internal.tasks.compile.incremental;

import java.io.File;

import static org.gradle.util.GFileUtils.relativePath;

/**
 * by Szczepan Faber, created at: 1/16/14
 */
public class ClassNameProvider {

    private final File compiledClassesDir;

    public ClassNameProvider(File compiledClassesDir) {
        this.compiledClassesDir = compiledClassesDir;
    }

    public String provideName(File classFile) {
        String path = relativePath(compiledClassesDir, classFile);
        if (path.startsWith("/") || path.startsWith(".")) {
            throw new IllegalArgumentException("Given input class file: '" + classFile + "' is not located inside of '" + compiledClassesDir + "'.");
        }
        return path.replaceAll("/", ".").replaceAll("\\.class", "");
    }
}