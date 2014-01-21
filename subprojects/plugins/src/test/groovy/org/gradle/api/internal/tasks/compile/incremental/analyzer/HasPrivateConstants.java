package org.gradle.api.internal.tasks.compile.incremental.analyzer;

/**
 * by Szczepan Faber, created at: 1/21/14
 */
public class HasPrivateConstants {
    private final static int x = 1;
    private final static HasNonPrivateConstants c = new HasNonPrivateConstants();
}
