package org.gradle.api.internal.tasks.compile.incremental

import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

/**
 * by Szczepan Faber, created at: 1/17/14
 */
class InputOutputMapperTest extends Specification {

    @Rule TestNameTestDirectoryProvider temp = new TestNameTestDirectoryProvider()

    def "knows input output mapping"() {
        when:
        InputOutputMapper mapper = new InputOutputMapper([temp.file("src/main/java"), temp.file("src/main/java2")], temp.file("out"))

        then:
        mapper.toOutputFile(temp.file("src/main/java/Foo.java")) == temp.file("out/Foo.class")
        mapper.toOutputFile(temp.file("src/main/java/org/bar/Bar.java")) == temp.file("out/org/bar/Bar.class")
        mapper.toOutputFile(temp.file("src/main/java2/com/Com.java")) == temp.file("out/com/Com.class")

        when:
        mapper.toOutputFile(temp.file("src/main/unknown/Xxx.java"))

        then:
        thrown(IllegalArgumentException)
    }
}