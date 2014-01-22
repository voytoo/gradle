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

            compileJava {
                def times = [:]
                doFirst {
                    fileTree("build/classes/main").each {
                        if (it.file) {
                            times[it] = it.lastModified()
                        }
                    }
                }
                doLast {
                    sleep(1100)
                    def changedFiles = ""
                    def unchangedFiles = ""
                    times.each { k,v ->
                        if (k.lastModified() != v) {
                            changedFiles += k.name + ","
                        } else {
                            unchangedFiles += k.name + ","
                        }
                    }
                    file("changedFiles.txt").text = changedFiles
                    file("unchangedFiles.txt").text = unchangedFiles
                }
            }
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

    Set getChangedFiles() {
        file("changedFiles.txt").text.split(",").findAll { it.length() > 0 }.collect { it.replaceAll("\\.class", "")}
    }

    Set getUnchangedFiles() {
        file("unchangedFiles.txt").text.split(",").findAll { it.length() > 0 }.collect { it.replaceAll("\\.class", "")}
    }

    def "only subset of output classes changes"() {
        when: run "compileJava"

        then:
        changedFiles.empty
        unchangedFiles.empty

        when:
        file("src/main/java/org/Person.java").text = """package org;
        public interface Person {
            String name();
        }"""
        file("src/main/java/org/PersonImpl.java").text = """package org;
        public class PersonImpl implements Person {
            public String name() { return "Szczepan"; }
        }"""

        run "compileJava"

        then:
        changedFiles == ['AnotherPersonImpl', 'PersonImpl', 'Person'] as Set
    }

    def "touches only the output class that was changed"() {
        run "compileJava"

        file("src/main/java/org/AnotherPersonImpl.java").text = """package org;
        public class AnotherPersonImpl implements Person {
            public String getName() { return "Hans"; }
        }"""

        when: run "compileJava"

        then: changedFiles == ['AnotherPersonImpl'] as Set
    }

    def "is sensitive to class deletion"() {
        run "compileJava"

        assert file("src/main/java/org/PersonImpl.java").delete()

        file("src/main/java/org/AnotherPersonImpl.java").text = """package org;
        public class AnotherPersonImpl implements Person {
            public String getName() { return "Hans"; }
        }"""

        when: run "compileJava"

        then:
        !file("build/classes/main/org/PersonImpl.class").exists()
        changedFiles == ['AnotherPersonImpl', 'PersonImpl'] as Set
    }

    def "is sensitive to inlined constants"() {
        run "compileJava"

        file("src/main/java/org/WithConst.java").text = """package org;
        public class WithConst {
            static final int X = 20;
        }"""

        when: run "compileJava"

        then:
        unchangedFiles.empty
        changedFiles.containsAll(['WithConst', 'AnotherPersonImpl', 'PersonImpl', 'Person'])
    }
}
