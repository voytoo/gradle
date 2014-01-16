package org.gradle.api.internal.tasks.compile.incremental

import spock.lang.Specification

/**
 * by Szczepan Faber, created at: 1/16/14
 */
class ClassDependencyTreeTest extends Specification {

    def "knows recursive dependency tree"() {
        def tree = new ClassDependencyTree(new File(ClassDependencyTreeTest.classLoader.getResource("").toURI()))
        expect:
        tree.getActualDependents(SomeClass.name) == [SomeOtherClass.name] as Set
        tree.getActualDependents(SomeOtherClass.name) == [] as Set
        tree.getActualDependents(YetAnotherClass.name) == [SomeOtherClass.name] as Set
        tree.getActualDependents(AccessedFromPrivateClass.name) == [SomeClass.name, SomeOtherClass.name] as Set
    }
}
