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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class ProgressOperations {

    private final Map<Long, LinkedList<ProgressOperation>> operations = new LinkedHashMap<Long, LinkedList<ProgressOperation>>();
    private LinkedList<ProgressOperation> recentOperations;
    private LinkedList<ProgressOperation> rootOperations;

    public void start(String description, String status, long groupId) {
        LinkedList ops = operations.get(groupId);
        if (ops == null) {
            ops = new LinkedList();
            operations.put(groupId, ops);
        }
        ops.addLast(new ProgressOperation(description, status));
        recentOperations = ops;
        if (rootOperations == null || rootOperations.isEmpty()) {
            rootOperations = ops;
        }
    }

    public void complete(long groupId) {
        LinkedList<ProgressOperation> op = operations.get(groupId);
        op.removeLast();
        if (op.isEmpty()) {
            operations.remove(groupId);
        }
        recentOperations = op;
    }

    public void progress(String description, long groupId) {
        LinkedList<ProgressOperation> op = operations.get(groupId);
        op.getLast().status = description;
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