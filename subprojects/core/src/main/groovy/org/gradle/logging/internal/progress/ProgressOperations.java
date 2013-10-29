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

package org.gradle.logging.internal.progress;

import org.gradle.logging.internal.ProgressCompleteEvent;
import org.gradle.logging.internal.ProgressEvent;
import org.gradle.logging.internal.ProgressStartEvent;

import java.util.LinkedList;
import java.util.List;

public class ProgressOperations {

    private final LinkedList<ProgressOperation> operations = new LinkedList<ProgressOperation>();

    public void start(ProgressStartEvent event) {
        operations.addLast(new ProgressOperation(event.getShortDescription(), event.getStatus()));
    }

    public void complete(ProgressCompleteEvent event) {
        operations.removeLast();
    }

    public void progress(ProgressEvent event) {
        operations.getLast().status = event.getStatus();
    }

    public List<ProgressOperation> getOperations() {
        return operations;
    }
}
