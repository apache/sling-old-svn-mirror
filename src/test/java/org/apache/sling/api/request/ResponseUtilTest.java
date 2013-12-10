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
package org.apache.sling.api.request;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ResponseUtilTest {
    
    private final String input;
    private final String expected;
    
    @Parameters
    public static Collection<Object[]> data() {
        final List<Object[]> result = new ArrayList<Object[]>();
        result.add(new Object[] { "", "" });
        result.add(new Object[] { "The quick brown fox runs over the lazy dog", null });
        result.add(new Object[] { "&><'\"", "&amp;&gt;&lt;&apos;&quot;" });
        result.add(new Object[] { "1&2>3<4'5\"6", "1&amp;2&gt;3&lt;4&apos;5&quot;6" });
        return result;
    }
    
    public ResponseUtilTest(String input, String expected) {
        this.input = input;
        this.expected = expected == null ? input : expected;
    }
    
    @Test
    public void testEscapeXml() {
        assertEquals(expected, ResponseUtil.escapeXml(input));
    }
    
    @Test
    public void testXmlWriter() throws IOException {
        final StringWriter w = new StringWriter();
        final Writer escaping = ResponseUtil.getXmlEscapingWriter(w);
        escaping.write(input);
        w.flush();
        assertEquals(expected, w.toString());
    }
}
