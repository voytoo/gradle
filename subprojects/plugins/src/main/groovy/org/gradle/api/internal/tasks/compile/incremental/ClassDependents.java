package org.gradle.api.internal.tasks.compile.incremental;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * by Szczepan Faber, created at: 1/21/14
 */
public class ClassDependents implements Serializable {

    private final Set<String> dependentClasses;

    public ClassDependents(Collection<String> dependentClasses) {
        if (dependentClasses != null) {
            this.dependentClasses = new LinkedHashSet<String>(dependentClasses);
        } else {
            this.dependentClasses = null;
        }
    }

    public Set<String> getDependentClasses() {
        return dependentClasses;
    }

    public boolean isDependentToAll() {
        return dependentClasses == null;
    }
}
