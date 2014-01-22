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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** SLING-3203 - POST servlet should not delete parent
 *  of non-existing node */
@RunWith(Parameterized.class)
public class PostServletDeleteParentTest {
    private final HttpTest H = new HttpTest();
    private static final String TEST_PATH = PostServletDeleteParentTest.class.getSimpleName() + "/" + System.currentTimeMillis();
    private final String deletePath;
    private final static String EXISTING_PATH = "test/some.node"; 
    
    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        final List<Object []> result = new ArrayList<Object []>();
        result.add(new Object[] { "test.other/nothing" });
        result.add(new Object[] { "test.other" });
        result.add(new Object[] { "test.html" });
        result.add(new Object[] { "test/some.node.html" });
        result.add(new Object[] { "test/some.node.selector.html" });
        result.add(new Object[] { "test/some.node.selector.html/another" });
        return result;
    }
    
    public PostServletDeleteParentTest(String deletePath) {
        this.deletePath = deletePath;
    }

    @Before
    public void setup() throws Exception {
        H.setUp();
    }
    
    @After
    public void cleanup() throws Exception {
        H.getTestClient().delete(HttpTest.HTTP_BASE_URL + "/" + TEST_PATH);
        H.tearDown();
    }
 
    @Test
    public void testDeleteNonExisting() throws Exception {
        final String path = TEST_PATH + "/" + EXISTING_PATH;
        final String testNodeUrl = H.getTestClient().createNode(HttpTest.HTTP_BASE_URL + "/" + path, null);
        assertTrue("Expecting created node path to end with " + path, testNodeUrl.endsWith(path));
        H.assertHttpStatus(testNodeUrl + ".json", 200, "Expecting test node to exist before test");

        // POST :delete to non-existing child node with a path that
        // generates selector + suffix
        final String selectorsPath = TEST_PATH + "/" + deletePath;
        final PostMethod post = new PostMethod(HttpTest.HTTP_BASE_URL + "/" + selectorsPath);
        post.setParameter(":operation",  "delete");
        final int status = H.getHttpClient().executeMethod(post);
        assertEquals("Expecting 403 status for delete operation", 403, status);

        // Test node should still be here
        H.assertHttpStatus(testNodeUrl + ".json", 200, "Expecting test node to exist after test");
    }
}