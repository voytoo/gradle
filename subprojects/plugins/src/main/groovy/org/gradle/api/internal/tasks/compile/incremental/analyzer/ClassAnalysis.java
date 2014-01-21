package org.gradle.api.internal.tasks.compile.incremental.analyzer;

import org.gradle.api.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * by Szczepan Faber, created at: 1/21/14
 */
public class ClassAnalysis {
    private final List<String> classDependencies;

    public ClassAnalysis(List<String> classDependencies) {
        this.classDependencies = classDependencies;
    }

    /**
     * Class dependencies, null if class is a dependent to all.
     */
    @Nullable
    public List<String> getClassDependencies() {
        return classDependencies;
    }
}
