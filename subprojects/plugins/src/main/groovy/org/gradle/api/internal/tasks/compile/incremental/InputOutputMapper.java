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

    public File toOutputFile(File inputJavaSource) {
        for (File sourceDir : sourceDirs) {
            if (inputJavaSource.getAbsolutePath().startsWith(sourceDir.getAbsolutePath())) { //perf tweak, this check is not 100% reliable
                String relativePath = GFileUtils.relativePath(sourceDir, inputJavaSource);
                if (!relativePath.startsWith("..")) {
                    String relativeClass = relativePath.replaceAll("\\.java$", ".class");
                    return new File(compileDestination, relativeClass);
                }
            }
        }
        throw new IllegalArgumentException(format("Unable to map input java source: '%s' because it does not belong to any of the source dirs: '%s'",
                inputJavaSource, sourceDirs));
    }
}
