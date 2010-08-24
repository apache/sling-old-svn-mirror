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
package org.apache.sling.launchpad.webapp.integrationtest.accessManager;

import java.io.IOException;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.launchpad.webapp.integrationtest.AbstractAuthenticatedTest;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Base class for AccessManager tests.
 */
public abstract class AbstractAccessManagerTest extends AbstractAuthenticatedTest {

	public static final String TEST_BASE_PATH = "/sling-tests";
    
    private static Random random = new Random(System.currentTimeMillis());
	
	protected String createTestFolder() throws IOException {
        String postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + "testFolder" + random.nextInt();

        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        return location;
	}
}
