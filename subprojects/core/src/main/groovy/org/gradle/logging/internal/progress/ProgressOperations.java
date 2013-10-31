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

import java.util.*;

public class ProgressOperations {

    private final Map<Long, LinkedList<ProgressOperation>> operations = new LinkedHashMap<Long, LinkedList<ProgressOperation>>();
    private LinkedList<ProgressOperation> recentlyChanged;
    private LinkedList<ProgressOperation> root;

    public void start(ProgressStartEvent event) {
        LinkedList ops = operations.get(event.getThreadId());
        if (ops == null) {
            ops = new LinkedList();
            operations.put(event.getThreadId(), ops);
        }
        ops.addLast(new ProgressOperation(event.getShortDescription(), event.getStatus()));
        recentlyChanged = ops;
        if (root == null || root.isEmpty()) {
            root = ops;
        }
    }

    public void complete(ProgressCompleteEvent event) {
        LinkedList<ProgressOperation> op = operations.get(event.getThreadId());
        op.removeLast();
        if (op.isEmpty()) {
            operations.remove(event.getThreadId());
        }
        recentlyChanged = op;
    }

    public void progress(ProgressEvent event) {
        LinkedList<ProgressOperation> op = operations.get(event.getThreadId());
        op.getLast().status = event.getStatus();
        recentlyChanged = op;
    }

    public Iterable<ProgressOperation> getOperations() {
        if (root == recentlyChanged) {
            return root;
        }
        return Iterables.concat(root, recentlyChanged);
    }

    public int getParallelOperationsCount() {
        return operations.size() - 2;
    }
}