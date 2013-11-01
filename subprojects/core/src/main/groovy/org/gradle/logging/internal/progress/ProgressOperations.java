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

import com.google.common.collect.Iterables;
import org.gradle.logging.internal.ProgressCompleteEvent;
import org.gradle.logging.internal.ProgressEvent;
import org.gradle.logging.internal.ProgressStartEvent;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class ProgressOperations {

    private final Map<Long, LinkedList<ProgressOperation>> operations = new LinkedHashMap<Long, LinkedList<ProgressOperation>>();
    private LinkedList<ProgressOperation> recentOperations;
    private LinkedList<ProgressOperation> rootOperations;

    public void start(ProgressStartEvent event) {
        LinkedList ops = operations.get(event.getGroupId());
        if (ops == null) {
            ops = new LinkedList();
            operations.put(event.getGroupId(), ops);
        }
        ops.addLast(new ProgressOperation(event.getShortDescription(), event.getStatus()));
        recentOperations = ops;
        if (rootOperations == null || rootOperations.isEmpty()) {
            rootOperations = ops;
        }
    }

    public void complete(ProgressCompleteEvent event) {
        LinkedList<ProgressOperation> op = operations.get(event.getGroupId());
        op.removeLast();
        if (op.isEmpty()) {
            operations.remove(event.getGroupId());
        }
        recentOperations = op;
    }

    public void progress(ProgressEvent event) {
        LinkedList<ProgressOperation> op = operations.get(event.getGroupId());
        op.getLast().status = event.getStatus();
        recentOperations = op;
    }

    public Iterable<ProgressOperation> getOperations() {
        if (rootOperations == recentOperations) {
            return rootOperations;
        }
        return Iterables.concat(rootOperations, recentOperations);
    }

    public int getParallelOperationsCount() {
        return operations.size() - 2;
    }
}