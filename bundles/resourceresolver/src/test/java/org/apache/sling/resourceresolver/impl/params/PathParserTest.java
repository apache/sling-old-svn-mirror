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

package org.apache.sling.resourceresolver.impl.params;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.resourceresolver.impl.params.PathParser;
import org.junit.Test;

public class PathParserTest {

    private PathParser parser = new PathParser();

    @Test
    public void nullParsesToNull() {
        parser.parse(null);
        assertNull(parser.getPath());
        assertTrue(parser.getParameters().isEmpty());
    }

    @Test
    public void emptyParsesToEmpty() {
        parser.parse("");
        assertEquals("", parser.getPath());
        assertTrue(parser.getParameters().isEmpty());
    }

    @Test
    public void noParametersReturnEmptyMap() {
        parser.parse("/path/with/no/parameters");
        assertEquals("/path/with/no/parameters", parser.getPath());
        assertTrue(parser.getParameters().isEmpty());
    }

    @Test
    public void parameterCanBeAddedAfterResource() {
        parser.parse("/content/test;key1=xyz");
        assertEquals("/content/test", parser.getPath());
        assertEquals(map("key1", "xyz"), parser.getParameters());

        parser.parse("/content/test;key1=xyz.html");
        assertEquals("/content/test.html", parser.getPath());
        assertEquals(map("key1", "xyz"), parser.getParameters());
    }

    @Test
    public void parameterCanBeEscaped() {
        parser.parse("/content/test;key1='xyz'");
        assertEquals("/content/test", parser.getPath());
        assertEquals(map("key1", "xyz"), parser.getParameters());

        parser.parse("/content/test;key1='xyz'.html");
        assertEquals("/content/test.html", parser.getPath());
        assertEquals(map("key1", "xyz"), parser.getParameters());
    }

    @Test
    public void multipleParametersAreAllowed() {
        parser.parse("/content/test;key1=xyz;key2=abc");
        assertEquals("/content/test", parser.getPath());
        assertEquals(map("key1", "xyz", "key2", "abc"), parser.getParameters());

        parser.parse("/content/test;key1=xyz;key2=abc.html");
        assertEquals("/content/test.html", parser.getPath());
        assertEquals(map("key1", "xyz", "key2", "abc"), parser.getParameters());
    }

    @Test
    public void multipleParametersCanBeEscaped() {
        parser.parse("/content/test;key1='a.b';key2='c.d'");
        assertEquals("/content/test", parser.getPath());
        assertEquals(map("key1", "a.b", "key2", "c.d"), parser.getParameters());

        parser.parse("/content/test;key1='a.b';key2='c.d'.html");
        assertEquals("/content/test.html", parser.getPath());
        assertEquals(map("key1", "a.b", "key2", "c.d"), parser.getParameters());
    }

    @Test
    public void parameterCanBeAddedAfterExtension() {
        parser.parse("/content/test.html;key1=xyz");
        assertEquals("/content/test.html", parser.getPath());
        assertEquals(map("key1", "xyz"), parser.getParameters());

        parser.parse("/content/test.html;key1=xyz/suffix");
        assertEquals("/content/test.html/suffix", parser.getPath());
        assertEquals(map("key1", "xyz"), parser.getParameters());

    }

    @Test
    public void dotDoesntHaveToBeEscapedAfterExtension() {
        parser.parse("/content/test.html;v=1.0");
        assertEquals("/content/test.html", parser.getPath());
        assertEquals(map("v", "1.0"), parser.getParameters());

        parser.parse("/content/test.html;v=1.0/suffix");
        assertEquals("/content/test.html/suffix", parser.getPath());
        assertEquals(map("v", "1.0"), parser.getParameters());
    }

    @Test
    public void slashHaveToBeEscapedAfterExtension() {
        parser.parse("/content/test.html;v='1/0'");
        assertEquals("/content/test.html", parser.getPath());
        assertEquals(map("v", "1/0"), parser.getParameters());

        parser.parse("/content/test.html;v='1/0'/suffix");
        assertEquals("/content/test.html/suffix", parser.getPath());
        assertEquals(map("v", "1/0"), parser.getParameters());
    }

    @Test
    public void quoteHasToBeClosed() {
        testInvalidParams("/content/test;key1='a.b;key2=cde.html/asd");
    }

    @Test
    public void emptyValueIsAllowed() {
        parser.parse("/content/test;key1=''.html");
        assertEquals("/content/test.html", parser.getPath());
        assertEquals(map("key1", ""), parser.getParameters());

        parser.parse("/content/test;key1=.html");
        assertEquals("/content/test.html", parser.getPath());
        assertEquals(map("key1", ""), parser.getParameters());
    }

    @Test
    public void parametersWithoutEqualsSignAreInvalid() {
        testInvalidParams("/content/test;key1.html");
    }

    @Test
    public void parametersInTheMiddleOfThePathAreInvalid() {
        testInvalidParams("/content;key=value/test.html");
    }

    @Test
    public void parametersInTheSuffixAreInvalid() {
        testInvalidParams("/content/test.html/suffix;key=value");
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> m = new HashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) {
            m.put(values[i], values[i + 1]);
        }
        return m;
    }

    private void testInvalidParams(String path) {
        parser.parse(path);
        assertEquals(path, parser.getPath());
        assertEquals(map(), parser.getParameters());
    }
}
