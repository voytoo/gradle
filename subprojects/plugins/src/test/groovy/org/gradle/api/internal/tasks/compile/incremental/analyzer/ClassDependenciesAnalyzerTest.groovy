package org.gradle.api.internal.tasks.compile.incremental.analyzer

import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.SomeClassAnnotation
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.SomeRuntimeAnnotation
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.SomeSourceAnnotation
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.UsesAnnotationInField
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.UsesClassAnnotation
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.UsesRuntimeAnnotation
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.UsesSourceAnnotation
import spock.lang.Specification
import spock.lang.Subject

/**
 * by Szczepan Faber, created at: 1/16/14
 */
class ClassDependenciesAnalyzerTest extends Specification {

    @Subject analyzer = new ClassDependenciesAnalyzer()

    private ClassAnalysis analyze(Class foo) {
        analyzer.getClassAnalysis(foo.name, classStream(foo))
    }

    def "knows dependencies of a java class"() {
        expect:
        analyze(SomeOtherClass).classDependencies == [YetAnotherClass.name, SomeClass.name]
    }

    def "knows basic class dependencies of a groovy class"() {
        def deps = analyze(ClassDependenciesAnalyzerTest).classDependencies

        expect:
        deps.contains(Specification.class.name)
        //deps.contains(ClassDependenciesAnalyzer.class.name) // why this does not work (is it because of groovy)?
    }

    def "knows if a class have non-private constants"() {
        expect:
        analyze(HasNonPrivateConstants).classDependencies == [UsedByNonPrivateConstantsClass.name]
        analyze(HasNonPrivateConstants).dependentToAll

        analyze(HasPublicConstants).classDependencies == []
        analyze(HasPublicConstants).dependentToAll

        analyze(HasPrivateConstants).classDependencies == [HasNonPrivateConstants.name]
        !analyze(HasPrivateConstants).dependentToAll
    }

    def "knows if a class uses annotations"() {
        expect:
        analyze(UsesRuntimeAnnotation).classDependencies == []
        analyze(SomeRuntimeAnnotation).classDependencies == []
        analyze(SomeRuntimeAnnotation).dependentToAll

        analyze(UsesClassAnnotation).classDependencies == []
        analyze(SomeClassAnnotation).classDependencies == []
        analyze(SomeClassAnnotation).dependentToAll

        analyze(UsesSourceAnnotation).classDependencies == [] //source annotations are wiped from the bytecode
        analyze(SomeSourceAnnotation).classDependencies == []
        analyze(SomeSourceAnnotation).dependentToAll
    }

    InputStream classStream(Class aClass) {
        aClass.getResourceAsStream(aClass.getSimpleName() + ".class")
    }
}
