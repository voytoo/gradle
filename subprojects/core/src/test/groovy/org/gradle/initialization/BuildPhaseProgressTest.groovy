package org.gradle.initialization

import spock.lang.Specification

/**
 * By Szczepan Faber on 7/5/13
 */
class BuildPhaseProgressTest extends Specification {

    def "knows progress"() {
        def progress = new BuildPhaseProgress("Building", 3);

        expect:
        progress.progress() == "Building 33%"
        progress.progress() == "Building 66%"
        progress.progress() == "Building 100%"

        when:
        progress.progress()

        then:
        thrown(IllegalStateException)
    }
}
