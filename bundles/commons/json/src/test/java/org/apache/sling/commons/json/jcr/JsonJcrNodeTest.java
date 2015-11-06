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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Test;

/**
 * @author vidar@idium.no
 * @since Apr 17, 2009 6:57:04 PM
 */
public class JsonJcrNodeTest {

    @Test
    public void testJcrJsonObject() throws RepositoryException, JSONException {
        MockNode node = new MockNode("/node1");
        node.setProperty("prop1", "value1");
        node.setProperty("prop2", "value2");
        Set<String> ignoredProperties = new HashSet<String>();
        ignoredProperties.add("prop2");
        JsonJcrNode json = new JsonJcrNode(node, ignoredProperties);
        assertTrue("Did not create property", json.has("prop1"));
        assertFalse("Created ignored property", json.has("prop2"));
        assertTrue("Did not create jcr:name", json.has("jcr:name"));
        assertTrue("Did not create jcr:path", json.has("jcr:path"));
    }

}
