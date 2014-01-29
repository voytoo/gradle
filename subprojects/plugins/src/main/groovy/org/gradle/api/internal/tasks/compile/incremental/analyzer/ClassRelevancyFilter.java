package org.gradle.api.internal.tasks.compile.incremental.analyzer;

/**
 * by Szczepan Faber, created at: 1/29/14
 */
public class ClassRelevancyFilter {

    private String excludedClassName;

    public ClassRelevancyFilter(String excludedClassName) {
        this.excludedClassName = excludedClassName;
    }

    boolean isRelevant(String className) {
        return !className.startsWith("java.") && !excludedClassName.equals(className);
    }
}