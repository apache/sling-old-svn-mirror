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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CallerFinderTest {

    @Test
    public void determineCallerSingle() throws Exception{
        CallerFinder cf = new CallerFinder(new String[] {"o.a.s", "o.a.j.o"});
        StackTraceElement[] stack = asStack(
                "o.a.j.o.a",
                "o.a.j.o.b",
                "c.a.g.w",
                "o.e.j",
                "o.e.j",
                "o.e.j"
        );

        StackTraceElement caller = cf.determineCaller(stack);
        assertNotNull(caller);
        assertEquals("c.a.g.w", caller.getClassName());
    }

    @Test
    public void determineCallerMultipleApi() throws Exception{
        CallerFinder cf = new CallerFinder(new String[] {"o.a.s", "o.a.j.o"});
        StackTraceElement[] stack = asStack(
                "o.a.j.o.a",
                "o.a.j.o.b",
                "o.a.s.a",
                "o.a.s.b",
                "c.a.g.w",
                "o.e.j",
                "o.e.j",
                "o.e.j"
        );

        StackTraceElement caller = cf.determineCaller(stack);
        assertNotNull(caller);
        assertEquals("c.a.g.w", caller.getClassName());

        stack = asStack(
                "o.a.j.o.a",
                "o.a.j.o.b",
                "o.a.s.a",
                "o.a.s.b",
                "c.a.g.w",
                "o.e.j",
                "o.e.j",
                "o.e.j"
        );

        cf = new CallerFinder(new String[] {"o.a.j.o"});
        caller = cf.determineCaller(stack);
        assertNotNull(caller);
        assertEquals("o.a.s.a", caller.getClassName());

    }

    @Test
    public void nullInput() throws Exception{
        CallerFinder cf = new CallerFinder(new String[] {"o.a.s", "o.a.j.o"});
        assertNull(cf.determineCaller(null));
    }

    @Test
    public void nullCaller() throws Exception{
        CallerFinder cf = new CallerFinder(new String[] {"o.a1.s", "o.a1.j.o"});
        StackTraceElement[] stack = asStack(
                "o.a.j.o.a",
                "o.a.j.o.b",
                "o.a.s.a",
                "o.a.s.b",
                "c.a.g.w",
                "o.e.j",
                "o.e.j",
                "o.e.j"
        );

        StackTraceElement caller = cf.determineCaller(stack);
        assertNull(caller);
    }

    static StackTraceElement[] asStack(String ... stack){
        StackTraceElement[] result = new StackTraceElement[stack.length];
        for (int i = 0; i < stack.length; i++) {
            result[i] = new StackTraceElement(stack[i], "foo", null, 0);
        }
        return result;
    }
}
