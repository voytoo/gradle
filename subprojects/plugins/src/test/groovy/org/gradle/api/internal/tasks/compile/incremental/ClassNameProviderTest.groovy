package org.gradle.api.internal.tasks.compile.incremental

import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Subject

/**
 * by Szczepan Faber, created at: 1/16/14
 */
class ClassNameProviderTest extends Specification {

    @Rule TestNameTestDirectoryProvider temp = new TestNameTestDirectoryProvider()
    @Subject provider = new ClassNameProvider(temp.createDir("root/dir"))

    def "provides class name"() {
        expect:
        "foo.bar.Foo" == provider.provideName(temp.file("root/dir/foo/bar/Foo.class"))
        "Foo" == provider.provideName(temp.file("root/dir/Foo.class"))
        'Foo$Bar' == provider.provideName(temp.file('root/dir/Foo$Bar.class'))
    }

    def "fails when class is outside of root"() {
        when:
        provider.provideName(temp.file("foo/Foo.class"))
        then:
        thrown(IllegalArgumentException)
    }
}
