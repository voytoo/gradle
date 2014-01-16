package org.gradle.java.compile

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

/**
 * by Szczepan Faber, created at: 1/15/14
 */
class TrueIncrementalJavaCompilationIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        buildFile << """
            apply plugin: 'java'
//            compileJava.options.fork = true
        """

        file("src/main/java/Person.java") << """public interface Person {
            String getName();
        }"""
        file("src/main/java/PersonImpl.java") << """public class PersonImpl implements Person {
            public String getName() { return "Szczepan"; }
        }"""
        file("src/main/java/AnotherPersonImpl.java") << """public class AnotherPersonImpl extends PersonImpl {
            public String getName() { return "Szczepan Faber"; }
        }"""
    }

    def "does not change the output files when no input has changed"() {
        when:
        run "compileJava"
        def personTime = file("build/classes/main/Person.class").lastModified()
        def implTime = file("build/classes/main/PersonImpl.class").lastModified()

        and:
        sleep(1000)
        run "compileJava"
        def personTime2 = file("build/classes/main/Person.class").lastModified()
        def implTime2 = file("build/classes/main/PersonImpl.class").lastModified()

        then:
        personTime == personTime2
        implTime == implTime2
    }

    def "changes the output files when all input has changed"() {
        when:
        run "compileJava"
        def personTime = file("build/classes/main/Person.class").lastModified()
        def implTime = file("build/classes/main/PersonImpl.class").lastModified()

        and:
        file("src/main/java/Person.java").text = """public interface Person {
            String name();
        }"""
        file("src/main/java/PersonImpl.java").text = """public class PersonImpl implements Person {
            public String name() { return "Szczepan"; }
        }"""

        and:
        sleep(1000)
        run "compileJava"
        def personTime2 = file("build/classes/main/Person.class").lastModified()
        def implTime2 = file("build/classes/main/PersonImpl.class").lastModified()

        then:
        personTime != personTime2
        implTime != implTime2
    }

    def "touches only the output class that was changed"() {
        when:
        run "compileJava"
        def personTime = file("build/classes/main/Person.class").lastModified()
        def implTime = file("build/classes/main/PersonImpl.class").lastModified()

        and:
        file("src/main/java/PersonImpl.java").text = """public class PersonImpl implements Person {
            public String getName() { return "Hans"; }
        }"""

        and:
        sleep(1000)
        run "compileJava"
        def personTime2 = file("build/classes/main/Person.class").lastModified()
        def implTime2 = file("build/classes/main/PersonImpl.class").lastModified()

        then:
        implTime != implTime2
        personTime == personTime2
    }

    def "understands class dependencies"() {
        when:
        run "compileJava"
        def personTime = file("build/classes/main/Person.class").lastModified()
        def implTime = file("build/classes/main/PersonImpl.class").lastModified()
        def anotherImplTime = file("build/classes/main/AnotherPersonImpl.class").lastModified()

        and:
        file("src/main/java/PersonImpl.java").text = """public class PersonImpl implements Person {
            public String getName() { return "Hans"; }
        }"""

        and:
        sleep(1000)
        run "compileJava"
        def personTime2 = file("build/classes/main/Person.class").lastModified()
        def implTime2 = file("build/classes/main/PersonImpl.class").lastModified()
        def anotherImplTime2 = file("build/classes/main/AnotherPersonImpl.class").lastModified()

        then:
        implTime != implTime2
        anotherImplTime != anotherImplTime2
        personTime == personTime2
    }
}
