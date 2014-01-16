package org.gradle.api.internal.tasks.compile.incremental

import spock.lang.Specification
import spock.lang.Subject

/**
 * by Szczepan Faber, created at: 1/16/14
 */
class ClassDependenciesAnalyzerTest extends Specification {

    @Subject analyzer = new ClassDependenciesAnalyzer()

    def "knows dependencies of a java class"() {
        def deps = analyzer.getClassesUsedBy(classStream(SomeClass.class))
        expect:
        [Set.name, HashSet.name, List.name, LinkedList.name, String.name, Integer.name, SomeClass.name + '$Foo'].each {
            deps.contains(it)
        }
    }

    def "knows basic class dependencies of a groovy class"() {
        def deps = analyzer.getClassesUsedBy(classStream(ClassDependenciesAnalyzerTest.class))

        expect:
        deps.contains(Specification.class.name)
        deps.contains(InputStream.class.name)
        //deps.contains(ClassDependenciesAnalyzer.class.name) why this does not work?
    }

    InputStream classStream(Class aClass) {
        aClass.getResourceAsStream(aClass.getSimpleName() + ".class")
    }
}
