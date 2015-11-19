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
package org.apache.sling.commons.json.jcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockNodeIterator;
import org.junit.Test;

/** Test the JsonItemWriter */
public class JsonItemWriterTest {
    
    private final JsonItemWriter writer = new JsonItemWriter(null);
    
    private String getJson(Node n, int levels) throws RepositoryException, JSONException {
        final StringWriter sw = new StringWriter();
        writer.dump(n, sw, 0);
        return sw.toString();
    }
    
    @Test
    public void testBasicJson() throws RepositoryException, JSONException {
        final Node n = new MockNode("/test");
        n.setProperty("testprop", "1234");
        assertEquals("{\"testprop\":\"1234\"}",getJson(n, 0));
    }
    
    @Test
    public void testMultivalued() throws RepositoryException, JSONException {
        final Node n = new MockNode("/test");
        final String [] values = { "1234", "yes" };
        n.setProperty("testprop", values);
        assertEquals("{\"testprop\":[\"1234\",\"yes\"]}",getJson(n, 0));
    }

    /**
     * See <a href="https://issues.apache.org/jira/browse/SLING-924">SLING-924</a>
     */
    @Test
    public void testOutputIterator() throws JSONException, RepositoryException {
        MockNode node1 = new MockNode("/node1");
        MockNode node2 = new MockNode("/node2");
        node1.addNode("node3");
        node1.setProperty("name", "node1");
        node2.setProperty("name", "node2");
        final NodeIterator it = new MockNodeIterator(new Node[]{node1, node2});
        final StringWriter sw = new StringWriter();
        writer.dump(it, sw);
        Pattern testPattern = Pattern.compile("\\{(.[^\\}]*)\\}"); // Pattern to look for a {...}
        Matcher matcher = testPattern.matcher(sw.toString());
        assertTrue("Did not produce a JSON object", matcher.find()); // Find first JSON object
        assertTrue("Did not produce a 2nd JSON object", matcher.find()); // Find second JSON object
        assertFalse("Produced a JSON object for a child node", matcher.find()); // Assert no child node has been created
    }
}
