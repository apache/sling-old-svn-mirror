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

import java.util.List;

import org.junit.Test;

import static org.apache.sling.tracer.internal.CallerFinderTest.asStack;
import static org.junit.Assert.assertArrayEquals;

public class CallerStackReporterTest {

    @Test
    public void startAndStop() throws Exception {
        StackTraceElement[] s = asStack("0", "1", "2", "3", "4", "5");
        assertArrayEquals(new String[]{"0", "1", "2", "3"}, arr(new CallerStackReporter(4).report(s)));
        assertArrayEquals(new String[]{"0"}, arr(new CallerStackReporter(1).report(s)));
        assertArrayEquals(new String[]{"2", "3"}, arr(new CallerStackReporter(2, 4, CallerFilter.ALL).report(s)));
    }

    @Test
    public void filter() throws Exception{
        StackTraceElement[] s = asStack("0", "1", "2", "3", "4", "5");
        CallerFilter f = new CallerFilter() {
            @Override
            public boolean include(StackTraceElement ste) {
                String name = ste.getClassName();
                return name.equals("1") || name.equals("2");
            }
        };

        assertArrayEquals(new String[]{"1", "2"}, arr(new CallerStackReporter(0, 4, f).report(s)));
    }

    @Test
    public void prefixFilter() throws Exception{
        StackTraceElement[] s = asStack("a.b.c", "a.b.d", "f.g.h", "m.g.i", "4", "5");
        assertArrayEquals(new String[]{"a.b.c", "a.b.d", "f.g.h", "m.g.i"},
                arr(new CallerStackReporter(0, 4, CallerFilter.ALL).report(s)));

        CallerFilter f = PrefixExcludeFilter.from("a.b|f.g");
        assertArrayEquals(new String[]{"m.g.i"},
                arr(new CallerStackReporter(0, 4, f).report(s)));
    }

    private static String[] arr(List<StackTraceElement> list) {
        String[] result = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).getClassName();
        }
        return result;
    }

}