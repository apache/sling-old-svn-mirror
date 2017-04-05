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

import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.testing.integration.HttpAnyMethod;

import java.io.IOException;
import java.io.InputStream;


/*
 *    handler   ranking     identifier
 * -------------------------------------------
 * TestHandler1    3    test-io-handler-1-
 * TestHandler2    2    test-io-handler-11-
 * TestHandler3    1    test-io-handler-111-
 *
 */
public class SlingWebDavServletTest extends RenderingTestBase {

    private final String testDir = "/sling-test/" + getClass().getSimpleName() + System.currentTimeMillis();
    
    // TODO there was previously no /default and the test passed, with a // before the testDir path
    // - need to clarify if this was by design
    private final String testDirUrl = HTTP_BASE_URL + "/dav/default" + testDir;

    private final String HANDLER     = "test-io-handler-";
    private final String HANDLER_1   = "test-io-handler-1";
    private final String HANDLER_11  = "test-io-handler-11";
    private final String HANDLER_111 = "test-io-handler-111";

    private static final String NUMMY_DATA = "dummy-data";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testClient.mkdirs(HTTP_BASE_URL, testDir);
        uploadFile(getHandlerUrl(HANDLER_1), NUMMY_DATA);
        uploadFile(getHandlerUrl(HANDLER_11), NUMMY_DATA);
        uploadFile(getHandlerUrl(HANDLER_111), NUMMY_DATA);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        testClient.delete(testDirUrl);
    }

    public void testServiceRunning() throws IOException {
        final String url = getHandlerUrl(HANDLER_1);
        final HttpAnyMethod propfind = new HttpAnyMethod("PROPFIND",url);
        int status = httpClient.executeMethod(propfind);
        assertEquals("PROPFIND " + url + " must return status 207", 207, status);
        final String content = propfind.getResponseBodyAsString();
        assertContains(content, HANDLER);
    }

    public void testHandlerOrder() throws IOException {
        checkHandler(getHandlerUrl(HANDLER_1), getHandlerIdentifier(HANDLER_1));
        checkHandler(getHandlerUrl(HANDLER_11), getHandlerIdentifier(HANDLER_11));
        checkHandler(getHandlerUrl(HANDLER_111), getHandlerIdentifier(HANDLER_111));
    }

    //

    /**
     * @param handlerName
     * @param identifier The expected handler identifier to be returned as a property.
     * @throws IOException
     */
    private void checkHandler(String handlerName, String identifier) throws IOException {
        final HttpAnyMethod propfind = new HttpAnyMethod("PROPFIND",handlerName);
        int status = httpClient.executeMethod(propfind);
        assertEquals("PROPFIND " + handlerName + " must return status 207", 207, status);
        final String content = propfind.getResponseBodyAsString();
        assertContains(content, identifier);
    }

    private void uploadFile(String url, String content) throws IOException {
        InputStream is = null;
        try {
            is = IOUtils.toInputStream(content);
            testClient.upload(url, is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private String getHandlerUrl(String handlerName){
        return testDirUrl + "/" + handlerName;
    }

    private String getHandlerIdentifier(String handlerName){
        return handlerName + "-";
    }

}
