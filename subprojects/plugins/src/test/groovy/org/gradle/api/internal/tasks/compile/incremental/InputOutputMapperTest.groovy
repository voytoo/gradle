package org.gradle.api.internal.tasks.compile.incremental

import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Subject

/**
 * by Szczepan Faber, created at: 1/17/14
 */
class InputOutputMapperTest extends Specification {

    @Rule TestNameTestDirectoryProvider temp = new TestNameTestDirectoryProvider()
    @Subject mapper = new InputOutputMapper([temp.file("src/main/java"), temp.file("src/main/java2")], temp.file("out"))

    def "knows input output mapping"() {
        expect:
        mapper.toOutputFile(temp.file("src/main/java/Foo.java")) == temp.file("out/Foo.class")
        mapper.toOutputFile(temp.file("src/main/java/org/bar/Bar.java")) == temp.file("out/org/bar/Bar.class")
        mapper.toOutputFile(temp.file("src/main/java2/com/Com.java")) == temp.file("out/com/Com.class")

        when: mapper.toOutputFile(temp.file("src/main/unknown/Xxx.java"))
        then: thrown(IllegalArgumentException)
    }

    def "knows java source class name"() {
        expect:
        mapper.toJavaSourceClass(temp.file("src/main/java/Foo.java")).className == "Foo"
        mapper.toJavaSourceClass(temp.file("src/main/java/org/bar/Bar.java")).className == "org.bar.Bar"
        mapper.toJavaSourceClass(temp.file("src/main/java2/com/Com.java")).className == "com.Com"

        when: mapper.toJavaSourceClass(temp.file("src/main/unknown/Xxx.java"))
        then: thrown(IllegalArgumentException)
    }

    def "knows java source class relative path"() {
        expect:
        mapper.toJavaSourceClass(temp.file("src/main/java/Foo.java")).relativePath == "Foo.java"
        mapper.toJavaSourceClass(temp.file("src/main/java/org/bar/Bar.java")).relativePath == "org/bar/Bar.java"
        mapper.toJavaSourceClass(temp.file("src/main/java2/com/Com.java")).relativePath == "com/Com.java"

        when: mapper.toJavaSourceClass(temp.file("src/main/unknown/Xxx.java"))
        then: thrown(IllegalArgumentException)
    }

    def "infers java source class from name"() {
        temp.createFile("src/main/java/Foo.java")
        temp.createFile("src/main/java/org/bar/Bar.java")
        temp.createFile("src/main/java2/com/Com.java")
        temp.createFile("src/main/unknown/Xxx.java")

        expect:
        mapper.toJavaSourceClass("Foo").relativePath == "Foo.java"
        mapper.toJavaSourceClass("org.bar.Bar").relativePath == "org/bar/Bar.java"
        mapper.toJavaSourceClass("com.Com").relativePath == "com/Com.java"

        when: mapper.toJavaSourceClass(temp.file("unknown.Xxx"))
        then: thrown(IllegalArgumentException)
    }
}