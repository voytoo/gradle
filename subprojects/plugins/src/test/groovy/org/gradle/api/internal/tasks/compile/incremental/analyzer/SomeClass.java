package org.gradle.api.internal.tasks.compile.incremental.analyzer;

import java.util.*;

/**
 * by Szczepan Faber, created at: 1/16/14
 */
public class SomeClass {

    List<Integer> field = new LinkedList<Integer>();

    private Set<String> stuff(HashMap map) {
        System.out.println(new Foo());
        return new HashSet<String>();
    }

    private class Foo {
        public String toString() {
            return "" + new AccessedFromPrivateClass();
        }
    }
}