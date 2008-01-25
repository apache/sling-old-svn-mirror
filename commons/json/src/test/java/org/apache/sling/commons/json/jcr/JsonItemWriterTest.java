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

import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.testing.jcr.MockNode;

/** Test the JsonItemWriter */
public class JsonItemWriterTest extends TestCase {
    
    private final JsonItemWriter writer = new JsonItemWriter(null);
    
    private String getJson(Node n, int levels) throws RepositoryException, JSONException {
        final StringWriter sw = new StringWriter();
        writer.dump(n, sw, 0);
        return sw.toString();
    }
    
    public void testBasicJson() throws RepositoryException, JSONException {
        final Node n = new MockNode("/test");
        n.setProperty("testprop", "1234");
        assertEquals("{\"testprop\":\"1234\"}",getJson(n, 0));
    }
    
    public void testMultivalued() throws RepositoryException, JSONException {
        final Node n = new MockNode("/test");
        final String [] values = { "1234", "yes" };
        n.setProperty("testprop", values);
        assertEquals("{\"testprop\":[\"1234\",\"yes\"]}",getJson(n, 0));
    }
}
