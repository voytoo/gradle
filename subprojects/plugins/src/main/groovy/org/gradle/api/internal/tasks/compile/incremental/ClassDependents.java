package org.gradle.api.internal.tasks.compile.incremental;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * by Szczepan Faber, created at: 1/21/14
 */
public class ClassDependents implements Serializable {

    private final Set<String> dependentClasses = new LinkedHashSet<String>();
    private boolean dependentToAll;

    public Set<String> getDependentClasses() {
        return dependentClasses;
    }

    public boolean isDependentToAll() {
        return dependentToAll;
    }

    public void addClass(String className) {
        dependentClasses.add(className);
    }

    public void setDependentToAll() {
        dependentToAll = true;
    }
}
