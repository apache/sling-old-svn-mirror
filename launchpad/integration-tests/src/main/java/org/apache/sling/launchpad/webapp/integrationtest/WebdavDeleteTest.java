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
package org.apache.sling.launchpad.webapp.integrationtest;

import junitx.framework.Assert;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WebdavDeleteTest extends HttpTestBase{
    private final String testDir = "/sling-test/" + getClass().getSimpleName() + System.currentTimeMillis();
    private final String testDirUrl = HTTP_BASE_URL + testDir;

    private final String DEFAULT_HANDLER   = "default-delete-handler";
    private final String HANDLER_1   = "test-delete-handler-1";
    private final String HANDLER_2   = "test-delete-handler-2";

    private final String HANDLER_1_BKP = "backed-up-by-" + HANDLER_1;
    private final String HANDLER_2_BKP = "backed-up-by-" + HANDLER_2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testClient.mkdirs(HTTP_BASE_URL, testDir);

        Map<String, String> map = new HashMap<String, String>();
        map.put("jcr:primaryType", "nt:unstructured");
        testClient.createNode(HTTP_BASE_URL + testDir + "/" + HANDLER_1, map);
        testClient.createNode(HTTP_BASE_URL + testDir + "/" + HANDLER_2, map);
        testClient.createNode(HTTP_BASE_URL + testDir + "/" + DEFAULT_HANDLER, map);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        testClient.delete(testDirUrl);
    }

    @Test
    public void testDelete() {
        try {
            testClient.delete(HTTP_BASE_URL + testDir + "/" + HANDLER_1);
            testClient.delete(HTTP_BASE_URL + testDir + "/" + HANDLER_2);
            testClient.delete(HTTP_BASE_URL + testDir + "/" + DEFAULT_HANDLER);

            Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, testClient.get(HTTP_BASE_URL + testDir + "/" + HANDLER_1));
            Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, testClient.get(HTTP_BASE_URL + testDir + "/" + HANDLER_2));
            Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, testClient.get(HTTP_BASE_URL + testDir + "/" + DEFAULT_HANDLER));

            Assert.assertEquals(HttpServletResponse.SC_OK, testClient.get(HTTP_BASE_URL + testDir + "/" + HANDLER_1 + HANDLER_1_BKP + ".json"));
            Assert.assertEquals(HttpServletResponse.SC_OK, testClient.get(HTTP_BASE_URL + testDir + "/" + HANDLER_2 + HANDLER_2_BKP + ".json"));

        } catch (IOException e) {
            Assert.fail(e);
        }
    }

}
