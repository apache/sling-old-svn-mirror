/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlets.post;

import org.apache.sling.commons.testing.sling.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;

import javax.json.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class HtmlResponseTest {
    protected HtmlResponse res;

    @Before
    public void setUp() throws Exception {
        res = new HtmlResponse();
        res.setReferer("");
    }

    @Test
    public void testNoChangesOnError() throws Exception {
        res.onChange("modified", "argument1");
        res.setError(new Exception("some exception"));
        final StringWriter stringWriter = new StringWriter();
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse() {
            @Override
            public PrintWriter getWriter() throws IOException {
                return new PrintWriter(stringWriter);
            }
        };
        res.doSend(response);
        String output = stringWriter.toString();
        assertTrue(output.contains("<div id=\"ChangeLog\"></div>"));
    }

    @Test
    public void testChangesOnNoError() throws Exception {
        res.onChange("modified", "argument1");
        final StringWriter stringWriter = new StringWriter();
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse() {
            @Override
            public PrintWriter getWriter() throws IOException {
                return new PrintWriter(stringWriter);
            }
        };
        res.doSend(response);
        String output = stringWriter.toString();
        assertTrue(output.contains("<div id=\"ChangeLog\">&lt;pre&gt;modified(&quot;argument1&quot;);&lt;br/&gt;&lt;/pre&gt;</div>"));
    }
}
