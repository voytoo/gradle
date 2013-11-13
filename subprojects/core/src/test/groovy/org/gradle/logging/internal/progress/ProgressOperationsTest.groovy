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



package org.gradle.logging.internal.progress

import spock.lang.Specification

class ProgressOperationsTest extends Specification {

    def ops = new ProgressOperations()

    List<ProgressOperation> getOperations() {
        ops.operations as List
    }

    def "is empty by default"() {
        expect:
        operations.empty
    }

    def "contains single operation"() {
        when:
        ops.start("compile", null, 1)

        then:
        operations*.message == ["compile"]
    }

    def "the operation uses status first"() {
        when:
        ops.start("foo", "compiling now", 1)

        then:
        operations*.message == ["compiling now"]
    }

    def "contains multiple operations"() {
        when:
        ops.start("Building", "", 1)
        ops.start("Resolving", "", 1)

        then:
        operations*.message == ["Building", "Resolving"]
    }

    def "contains progress"() {
        when:
        ops.start("Building", "", 1)
        ops.start("Resolving", "", 1)
        ops.progress("Download", 1)

        then:
        operations*.message == ["Building", "Download"]
    }

    def "does not contain completed events"() {
        when:
        ops.start("Building", "", 1)
        ops.start("Resolving", "", 1)
        ops.progress("Download", 1)
        ops.complete(1)

        then:
        operations*.message == ["Building"]
    }

    def "is empty when everything completed"() {
        when:
        ops.start("Building", "", 1)
        ops.start("Resolving", "", 2)
        ops.progress("Download", 2)
        ops.complete(2)
        ops.complete(1)

        then:
        operations*.message == []
    }

    def "contains events from different group"() {
        when:
        ops.start("Building", "", 1)
        ops.start("task 1", "", 2)

        then:
        operations*.message == ["Building", "task 1"]
    }

    def "contains progress from different group"() {
        when:
        ops.start("Building", "", 1)
        ops.start("task 1", "", 2)
        ops.progress("compiling", 2)

        then:
        operations*.message == ["Building", "compiling"]
    }

    def "contains progress from interleaving groups"() {
        when:
        ops.start("Building", "", 1)
        ops.start("task 1", "", 2)
        ops.progress("compiling", 2)
        ops.start("task 2", "", 3)
        ops.progress("resolving", 3)

        then:
        operations*.message == ["Building", "resolving"]
    }

    def "excludes completed events from from interleaving groups"() {
        when:
        ops.start("Building", "", 1)
        ops.start("task 1", "", 2)
        ops.start("task 2", "", 3)

        ops.progress("compiling", 2)
        ops.progress("resolving", 3)
        ops.complete(3)

        then:
        operations*.message == ["Building", "compiling"]
    }

    def "shows most recent operation from given group even if it has completed progress"() {
        when:
        ops.start("Building", "", 1)
        ops.start("task 1", "", 2)
        ops.start("task 2", "", 3)
        ops.start("compiling", "", 3)
        ops.complete(3)

        then:
        operations*.message == ["Building", "task 2"]
    }
}