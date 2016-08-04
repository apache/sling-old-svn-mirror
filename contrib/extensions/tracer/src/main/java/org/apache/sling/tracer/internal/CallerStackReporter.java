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

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

class CallerStackReporter {
    /**
     * Package names which should be excluded from the stack as they do not serve
     * any purpose and bloat the size
     */
    private static final CallerFilter FWK_EXCLUDE_FILTER = new PrefixExcludeFilter(asList(
            "java.lang.Thread",
            "org.apache.sling.tracer.internal",
            "ch.qos.logback.classic",
            "sun.reflect",
            "java.lang.reflect"
    ));
    private final CallerFilter callerFilter;
    private final int start;
    private final int depth;

    public CallerStackReporter(int depth){
        this(0, depth, CallerFilter.ALL);
    }

    public CallerStackReporter(int start, int depth, CallerFilter filter){
        this.start = start;
        this.depth = depth;
        this.callerFilter = filter;
    }

    public List<StackTraceElement> report(){
        return report(Thread.currentThread().getStackTrace());
    }

    public List<StackTraceElement> report(StackTraceElement[] stack){
        List<StackTraceElement> filteredStack = fwkExcludedStack(stack);
        List<StackTraceElement> result = new ArrayList<StackTraceElement>(filteredStack.size());

        //Iterate over the filtered stack with limits applicable on that not on actual stack
        for (int i = 0; i < filteredStack.size(); i++) {
            StackTraceElement ste = filteredStack.get(i);
            if (i >=  start && i < depth
                    && callerFilter.include(ste)){
                result.add(ste);
            }
        }
        return result;
    }

    private List<StackTraceElement> fwkExcludedStack(StackTraceElement[] stack) {
        List<StackTraceElement> filteredStack = new ArrayList<StackTraceElement>(stack.length);
        for (StackTraceElement ste : stack) {
            if (FWK_EXCLUDE_FILTER.include(ste)){
                filteredStack.add(ste);
            }
        }
        return filteredStack;
    }

    public CallerFilter getCallerFilter() {
        return callerFilter;
    }

    public int getStart() {
        return start;
    }

    public int getDepth() {
        return depth;
    }
}
