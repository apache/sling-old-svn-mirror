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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Test the SlingPostProcessor mechanism using the SlingPostProcessorOne
 *  and Two from our test-services module. 
 */
public class SlingPostProcessorTest {
    private HttpTest T = new HttpTest();
    private String testUrl;
    
    @Before
    public void setup() throws Exception {
        T.setUp();
        testUrl = HttpTest.HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + UUID.randomUUID();
    }
    
    @After
    public void cleanup() throws IOException {
        T.getTestClient().delete(testUrl);
    }
    
    @Test
    public void processorsActive() throws HttpException, IOException {
        final PostMethod post = new PostMethod(testUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX);
        post.setFollowRedirects(false);
        post.setParameter("DummyModification", "true");

        try {
            T.getHttpClient().executeMethod(post);
            final String content = post.getResponseBodyAsString();
            final int i1 = content.indexOf("source:SlingPostProcessorOne");
            assertTrue("Expecting first processor to be present", i1 > 0);
            final int i2 = content.indexOf("source:SlingPostProcessorTwo");
            assertTrue("Expecting second processor to be present", i2 > 0);
            assertTrue("Expecting service ranking to put processor one first", i1 < i2);
        } finally {
            
            post.releaseConnection();
        }

    }
}