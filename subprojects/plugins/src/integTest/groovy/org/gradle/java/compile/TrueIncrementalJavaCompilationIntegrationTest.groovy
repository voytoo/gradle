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

        file("src/main/java/org/Person.java") << """package org;
        public interface Person {
            String getName();
        }"""
        file("src/main/java/org/PersonImpl.java") << """package org;
        public class PersonImpl implements Person {
            public String getName() { return "Szczepan"; }
        }"""
        file("src/main/java/org/AnotherPersonImpl.java") << """package org;
        public class AnotherPersonImpl extends PersonImpl {
            public String getName() { return "Szczepan Faber " + WithConst.X; }
        }"""
        file("src/main/java/org/WithConst.java") << """package org;
        public class WithConst {
            final static int X = 100;
        }"""
    }

    def "does not change the output files when no input has changed"() {
        when:
        run "compileJava"
        def personTime = file("build/classes/main/org/Person.class").lastModified()
        def implTime = file("build/classes/main/org/PersonImpl.class").lastModified()

        and:
        sleep(1000)
        run "compileJava"
        def personTime2 = file("build/classes/main/org/Person.class").lastModified()
        def implTime2 = file("build/classes/main/org/PersonImpl.class").lastModified()

        then:
        personTime == personTime2
        implTime == implTime2
    }

    def "changes the output files when all input has changed"() {
        when:
        run "compileJava"
        def personTime = file("build/classes/main/org/Person.class").lastModified()
        def implTime = file("build/classes/main/org/PersonImpl.class").lastModified()

        and:
        file("src/main/java/org/Person.java").text = """package org;
        public interface Person {
            String name();
        }"""
        file("src/main/java/org/PersonImpl.java").text = """package org;
        public class PersonImpl implements Person {
            public String name() { return "Szczepan"; }
        }"""

        and:
        sleep(1000)
        run "compileJava"
        def personTime2 = file("build/classes/main/org/Person.class").lastModified()
        def implTime2 = file("build/classes/main/org/PersonImpl.class").lastModified()

        then:
        personTime != personTime2
        implTime != implTime2
    }

    def "touches only the output class that was changed"() {
        when:
        run "compileJava"
        def personTime = file("build/classes/main/org/Person.class").lastModified()
        def implTime = file("build/classes/main/org/PersonImpl.class").lastModified()

        and:
        file("src/main/java/PersonImpl.java").text = """package org;
        public class PersonImpl implements Person {
            public String getName() { return "Hans"; }
        }"""

        and:
        sleep(1000)
        run "compileJava"
        def personTime2 = file("build/classes/main/org/Person.class").lastModified()
        def implTime2 = file("build/classes/main/org/PersonImpl.class").lastModified()

        then:
        implTime != implTime2
        personTime == personTime2
    }

    def "understands class dependencies"() {
        when:
        run "compileJava"
        def personTime = file("build/classes/main/org/Person.class").lastModified()
        def implTime = file("build/classes/main/org/PersonImpl.class").lastModified()
        def anotherImplTime = file("build/classes/main/org/AnotherPersonImpl.class").lastModified()

        and:
        file("src/main/java/org/PersonImpl.java").text = """package org;
        public class PersonImpl implements Person {
            public String getName() { return "Hans"; }
        }"""

        and:
        sleep(1000)
        run "compileJava"
        def personTime2 = file("build/classes/main/org/Person.class").lastModified()
        def implTime2 = file("build/classes/main/org/PersonImpl.class").lastModified()
        def anotherImplTime2 = file("build/classes/main/another/org/AnotherPersonImpl.class").lastModified()

        then:
        implTime != implTime2
        anotherImplTime != anotherImplTime2
        personTime == personTime2
    }

    def "is sensitive to class deletion"() {
        when:
        run "compileJava"
        def personTime = file("build/classes/main/org/Person.class").lastModified()
        def anotherImplTime = file("build/classes/main/org/AnotherPersonImpl.class").lastModified()

        and:
        assert file("src/main/java/org/PersonImpl.java").delete()
        file("src/main/java/org/AnotherPersonImpl.java").text = """package org;
        public class AnotherPersonImpl implements Person {
            public String getName() { return "Hans"; }
        }"""

        and:
        sleep(1000)
        run "compileJava"
        def personTime2 = file("build/classes/main/org/Person.class").lastModified()
        def anotherImplTime2 = file("build/classes/main/org/AnotherPersonImpl.class").lastModified()

        then:
        !file("build/classes/main/org/PersonImpl.class").exists()
        anotherImplTime != anotherImplTime2
        personTime == personTime2
    }

    def "is sensitive to inlined constants"() {
        when:
        run "compileJava"
        def withConstTime = file("build/classes/main/org/WithConst.class").lastModified()
        def anotherImplTime = file("build/classes/main/org/AnotherPersonImpl.class").lastModified()

        and:
        file("src/main/java/org/WithConst.java").text = """package org;
        public class WithConst {
            static final int X = 20;
        }"""

        and:
        sleep(1000)
        run "compileJava"
        def withConstTime2 = file("build/classes/main/org/WithConst.class").lastModified()
        def anotherImplTime2 = file("build/classes/main/org/AnotherPersonImpl.class").lastModified()

        then:
        withConstTime != withConstTime2
        anotherImplTime != anotherImplTime2
    }
}
