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

package org.apache.sling.commons.log.logback.internal;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.sling.commons.log.logback.internal.Tailer.TailerListener;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class FilteringListenerTest {

    private StringWriter sw = new StringWriter();
    private PrintWriter pw = new PrintWriter(sw);

    @Test
    public void nullPattern() throws Exception{
        TailerListener l = new FilteringListener(pw, null);
        l.handle("foo");
        assertThat(sw.toString(), containsString("foo"));
    }

    @Test
    public void allPattern() throws Exception{
        TailerListener l = new FilteringListener(pw, FilteringListener.MATCH_ALL);
        l.handle("foo");
        assertThat(sw.toString(), containsString("foo"));
    }

    @Test
    public void basicPattern() throws Exception{
        TailerListener l = new FilteringListener(pw, "foo");
        l.handle("foo");
        assertThat(sw.toString(), containsString("foo"));

        l.handle("bar");
        assertThat(sw.toString(), not(containsString("bar")));

        l.handle("foo bar");
        assertThat(sw.toString(), containsString("foo bar"));
    }

    @Test
    public void regexPattern() throws Exception{
        TailerListener l = new FilteringListener(pw, "foo.*bar");
        l.handle("foo");
        assertThat(sw.toString(), not(containsString("foo")));

        l.handle("bar");
        assertThat(sw.toString(), not(containsString("bar")));

        l.handle("foo talks to bar");
        assertThat(sw.toString(), containsString("bar"));
        assertThat(sw.toString(), containsString("talks"));
    }

}