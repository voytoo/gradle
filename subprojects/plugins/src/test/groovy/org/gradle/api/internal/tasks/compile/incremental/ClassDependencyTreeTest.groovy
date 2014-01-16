package org.gradle.api.internal.tasks.compile.incremental

import spock.lang.Specification

/**
 * by Szczepan Faber, created at: 1/16/14
 */
class ClassDependencyTreeTest extends Specification {

    def "knows dependency tree"() {
        def tree = new ClassDependencyTree(new File(ClassDependencyTreeTest.classLoader.getResource("").toURI()))
        expect:
        println tree.getDependents(SomeClass.name)
    }
}
