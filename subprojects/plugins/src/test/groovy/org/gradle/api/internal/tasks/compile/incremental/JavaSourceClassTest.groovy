package org.gradle.api.internal.tasks.compile.incremental

import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

/**
 * by Szczepan Faber, created at: 1/17/14
 */
class JavaSourceClassTest extends Specification {

    @Rule TestNameTestDirectoryProvider temp = new TestNameTestDirectoryProvider()

    def "knows output file"() {
        expect:
        new JavaSourceClass("com/Foo.java", temp.file("dir")).outputFile == temp.file("dir/com/Foo.class")
        new JavaSourceClass("Foo.java", temp.file("dir")).outputFile == temp.file("dir/Foo.class")
    }

    def "knows class name"() {
        expect:
        new JavaSourceClass("com/Foo.java", temp.file("dir")).className == "com.Foo"
        new JavaSourceClass("Foo.java", temp.file("dir")).className == "Foo"
    }
}
