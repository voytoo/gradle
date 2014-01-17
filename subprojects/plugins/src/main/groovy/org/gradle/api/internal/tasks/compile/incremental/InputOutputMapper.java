package org.gradle.api.internal.tasks.compile.incremental;

import org.gradle.util.GFileUtils;

import java.io.File;

import static java.lang.String.format;

/**
 * by Szczepan Faber, created at: 1/17/14
 */
public class InputOutputMapper {

    private Iterable<File> sourceDirs;
    private File compileDestination;

    public InputOutputMapper(Iterable<File> sourceDirs, File compileDestination) {
        this.sourceDirs = sourceDirs;
        this.compileDestination = compileDestination;
    }

    public JavaSourceClass toJavaSourceClass(File javaSourceClass) {
        for (File sourceDir : sourceDirs) {
            if (javaSourceClass.getAbsolutePath().startsWith(sourceDir.getAbsolutePath())) { //perf tweak only
                String relativePath = GFileUtils.relativePath(sourceDir, javaSourceClass);
                if (!relativePath.startsWith("..")) {
                    return new JavaSourceClass(relativePath, compileDestination);
                }
            }
        }
        throw new IllegalArgumentException(format("Unable to find source java class: '%s' because it does not belong to any of the source dirs: '%s'",
                javaSourceClass, sourceDirs));

    }

    public JavaSourceClass toJavaSourceClass(String className) {
        String relativePath = className.replaceAll("\\.", "/").concat(".java");
        for (File sourceDir : sourceDirs) {
            if (new File(sourceDir, relativePath).isFile()) {
                return new JavaSourceClass(relativePath, compileDestination);
            }
        }
        throw new IllegalArgumentException(format("Unable to find source java class for '%s'. The source file '%s' was not found in source dirs: '%s'",
                className, relativePath, sourceDirs));
    }
}
