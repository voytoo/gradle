/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.compile.daemon

import org.gradle.api.internal.tasks.compile.CompileSpec
import org.gradle.process.internal.WorkerProcess
import org.gradle.util.SetSystemProperties
import org.junit.Rule
import spock.lang.Specification

import java.util.concurrent.SynchronousQueue

class CompilerDaemonClientTest extends Specification {

    @Rule SetSystemProperties p = new SetSystemProperties()

    def "daemons can be retired by system property"() {
        System.setProperty("tiredThreshold", "5")

        def client = new CompilerDaemonClient(Stub(DaemonForkOptions), Stub(WorkerProcess),
                Mock(CompilerDaemonServerProtocol), Mock(SynchronousQueue));

        def compiler = Stub(org.gradle.api.internal.tasks.compile.Compiler)
        def compileSpec = Stub(CompileSpec)

        when: 5.times { client.execute(compiler, compileSpec); }

        then: !client.tired

        when: client.execute(compiler, compileSpec)

        then: client.tired
    }
}
