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
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptySet;

public class ProgressOperations {

    private final Map<Long, LinkedList<ProgressOperation>> operations = new LinkedHashMap<Long, LinkedList<ProgressOperation>>();
    private LinkedList<Long> groups = new LinkedList<Long>();

    public void start(String description, String status, long groupId) {
        LinkedList ops = operations.get(groupId);
        if (ops == null) {
            ops = new LinkedList();
            operations.put(groupId, ops);
        }
        ops.addLast(new ProgressOperation(description, status));
        markCurrentGroup(groupId);
    }

    private void markCurrentGroup(long groupId) {
        groups.remove(groupId);
        groups.addLast(groupId);
    }

    public void complete(long groupId) {
        LinkedList<ProgressOperation> op = operations.get(groupId);
        op.removeLast();
        if (op.isEmpty()) {
            operations.remove(groupId);
            groups.remove(groupId);
        }
    }

    public void progress(String description, long groupId) {
        LinkedList<ProgressOperation> op = operations.get(groupId);
        op.getLast().status = description;
        markCurrentGroup(groupId);
    }

    public Iterable<ProgressOperation> getOperations() {
        if (groups.isEmpty()) {
            return emptySet();
        }
        List recent = operations.get(this.groups.getLast());
        List root = operations.get(this.groups.getFirst());
        if (root == recent) {
            return root;
        }
        return Iterables.concat(root, recent);
    }

    public int getParallelOperationsCount() {
        return operations.size() - 2;
    }
}