package org.gradle.api.internal.tasks.compile.incremental;

import java.util.Set;

/**
 * by Szczepan Faber, created at: 1/21/14
 */
public class ClassDependents {

    private Set<String> dependentClasses;

    public ClassDependents(Set<String> dependentClasses) {
        this.dependentClasses = dependentClasses;
    }

    public Set<String> getDependentClasses() {
        return dependentClasses;
    }
}
