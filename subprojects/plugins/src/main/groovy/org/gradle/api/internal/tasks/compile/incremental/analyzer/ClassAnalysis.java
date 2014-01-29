package org.gradle.api.internal.tasks.compile.incremental.analyzer;

import java.util.List;

/**
 * by Szczepan Faber, created at: 1/21/14
 */
public class ClassAnalysis {
    private final List<String> classDependencies;
    private boolean dependentToAll;

    public ClassAnalysis(List<String> classDependencies, boolean dependentToAll) {
        this.classDependencies = classDependencies;
        this.dependentToAll = dependentToAll;
    }

    public List<String> getClassDependencies() {
        return classDependencies;
    }

    public boolean isDependentToAll() {
        return dependentToAll;
    }
}
