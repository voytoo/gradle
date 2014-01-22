package org.gradle.api.internal.tasks.compile.incremental.analyzer

import spock.lang.Specification
import spock.lang.Subject

/**
 * by Szczepan Faber, created at: 1/16/14
 */
class ClassDependenciesAnalyzerTest extends Specification {

    @Subject analyzer = new ClassDependenciesAnalyzer()

    def "knows dependencies of a java class"() {
        def deps = analyzer.getClassAnalysis(SomeOtherClass.name, classStream(SomeOtherClass)).classDependencies
        expect:
        deps == [YetAnotherClass.name, SomeClass.name]
    }

    def "knows basic class dependencies of a groovy class"() {
        def deps = analyzer.getClassAnalysis(ClassDependenciesAnalyzerTest.name, classStream(ClassDependenciesAnalyzerTest)).classDependencies

        expect:
        deps.contains(Specification.class.name)
        //deps.contains(ClassDependenciesAnalyzer.class.name) // why this does not work (is it because of groovy)?
    }

    def "knows if a class have non-private constants"() {
        expect:
        analyzer.getClassAnalysis(HasNonPrivateConstants.name, classStream(HasNonPrivateConstants)).getClassDependencies() == [UsedByNonPrivateConstantsClass.name]
        analyzer.getClassAnalysis(HasNonPrivateConstants.name, classStream(HasNonPrivateConstants)).dependentToAll

        analyzer.getClassAnalysis(HasPublicConstants.name, classStream(HasPublicConstants)).getClassDependencies() == []
        analyzer.getClassAnalysis(HasPublicConstants.name, classStream(HasPublicConstants)).dependentToAll

        analyzer.getClassAnalysis(HasPrivateConstants.name, classStream(HasPrivateConstants)).getClassDependencies() == [HasNonPrivateConstants.name]
        !analyzer.getClassAnalysis(HasPrivateConstants.name, classStream(HasPrivateConstants)).dependentToAll
    }

    def "knows if a class is a source annotation"() {

    }

    InputStream classStream(Class aClass) {
        aClass.getResourceAsStream(aClass.getSimpleName() + ".class")
    }
}
