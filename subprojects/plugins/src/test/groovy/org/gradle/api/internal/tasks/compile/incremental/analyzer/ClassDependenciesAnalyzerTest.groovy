package org.gradle.api.internal.tasks.compile.incremental.analyzer

import spock.lang.Specification
import spock.lang.Subject

/**
 * by Szczepan Faber, created at: 1/16/14
 */
class ClassDependenciesAnalyzerTest extends Specification {

    @Subject analyzer = new ClassDependenciesAnalyzer()

    def "knows dependencies of a java class"() {
        def deps = analyzer.getClassAnalysis(classStream(SomeClass.class)).classDependencies
        expect:
        println deps
        [Set.name, HashSet.name, List.name, LinkedList.name, String.name, Integer.name, SomeClass.name + '$Foo'].each {
            deps.contains(it)
        }
    }

    def "knows basic class dependencies of a groovy class"() {
        def deps = analyzer.getClassAnalysis(classStream(ClassDependenciesAnalyzerTest.class)).classDependencies

        expect:
        deps.contains(Specification.class.name)
        deps.contains(InputStream.class.name)
        //deps.contains(ClassDependenciesAnalyzer.class.name) // why this does not work (is it because of groovy)?
    }

    def "knows if a class have non-private constants"() {
        expect:
        analyzer.getClassAnalysis(classStream(HasNonPrivateConstants)).getClassDependencies() == null

        analyzer.getClassAnalysis(classStream(HasPublicConstants)).getClassDependencies() == null

        analyzer.getClassAnalysis(classStream(HasPrivateConstants)).getClassDependencies() == [HasNonPrivateConstants.name, HasPrivateConstants.name]
    }

    def "knows if a class is a source annotation"() {

    }

    InputStream classStream(Class aClass) {
        aClass.getResourceAsStream(aClass.getSimpleName() + ".class")
    }
}
