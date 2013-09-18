package org.gradle.api

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import spock.lang.IgnoreIf

//execute only for daemon and embedded
@IgnoreIf({ !(GradleContextualExecuter.daemon || GradleContextualExecuter.embedded)})
class CachingClassloadersIntegrationTest extends AbstractIntegrationSpec {

    def "classloaders are cached within a process"() {
        requireOwnGradleUserHomeDir() //so that it is executed within the same daemon

        buildFile << """
            class Foo {
                static String message
            }

            task setMessage << {
                Foo.message = 'hey!'
            }
            task assertMessage << {
                println "Class: " + Foo.class + ", loader: " + Foo.class.classLoader
                println "Class: " + JavaPlugin.class + ", loader: " + JavaPlugin.class.classLoader
                assert Foo.message == 'hey!'
            }
        """

        run "setMessage", "assertMessage"

        expect:
        //static state should be preserved within the same process
        run "assertMessage"
    }

    def "classloaders are cached for multi project builds"() {
        requireOwnGradleUserHomeDir() //so that it is executed within the same daemon

        settingsFile << "include 'a', 'b'"
        buildFile << """
            class Foo {
                static String message
            }
            allprojects {
                task setMessage << {
                    Foo.message = 'hey!'
                }
                task assertMessage << {
                    println "Class: " + Foo.class + ", loader: " + Foo.class.classLoader
                    println "Class: " + JavaPlugin.class + ", loader: " + JavaPlugin.class.classLoader
                    assert Foo.message == 'hey!'
                }
            }
        """

        file("a/build.gradle") << """
            println "Configuring a"
        """

        file("b/build.gradle") << """
            println "Configuring b"
        """

        run "setMessage", "assertMessage"

        expect:
        //static state should be preserved within the same process
        run "assertMessage"
    }

}
