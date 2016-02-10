/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.tracer.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

/**
 * Utility to find out the real caller by excluding stack elements belonging to
 * API classes. Say for a query it would exclude the call stack which is part of Oak
 * or Sling Engine
 */
class CallerFinder {
    private final String[] apiPkgs;

    public CallerFinder(String[] apiPkgs) {
        this.apiPkgs = apiPkgs;
    }

    @CheckForNull
    public StackTraceElement determineCaller(StackTraceElement[] stack) {
        if (stack == null) {
            return null;
        }

        //Reverse the stack trace so as to start from bottom
        List<StackTraceElement> stackList = Arrays.asList(stack);
        Collections.reverse(stackList);
        PeekingIterator<StackTraceElement> pit = Iterators.peekingIterator(stackList.iterator());
        while (pit.hasNext()) {
            StackTraceElement current = pit.next();

            //now scan each element and check if the *next* stack element belongs to any
            //api package. If yes then current stack would be the caller
            if (pit.hasNext()) {
                StackTraceElement next = pit.peek();
                for (String pkg : apiPkgs) {
                    if (next.getClassName().startsWith(pkg)) {
                        return current;
                    }
                }
            }
        }
        return null;
    }
}
