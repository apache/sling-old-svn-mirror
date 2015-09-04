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

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.httpclient.HttpException;
import org.apache.sling.commons.testing.integration.HttpAnyMethod;
import org.apache.sling.commons.testing.integration.HttpTest;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Clarify the behavior of a double slash in a Sling WebDAV URL */
public class WebDavDoubleSlashTest {
    private final HttpTest H = new HttpTest();
    String testPath;
    String toDelete;
    
    @Before
    public void setup() throws Exception {
        H.setUp();
        testPath = "/test/" + getClass().getSimpleName() + "/" + UUID.randomUUID();
        toDelete = H.getTestClient().createNode(HttpTest.HTTP_BASE_URL + testPath, null);
    }
    
    @After
    public void cleanup() throws Exception {
        H.tearDown();
        H.getTestClient().delete(toDelete);
    }
    
    /** Do a PROPFIND on /dav, adding worskpace name after that as required by the Jackrabbit WebDAV modules */
    private int getPropfindStatus(String workspaceName, String nodePath) throws HttpException, IOException {
        final String webdavRoot = "/dav";
        final String url = HttpTest.HTTP_BASE_URL + webdavRoot + "/" + workspaceName + nodePath;
        final HttpAnyMethod propfind = new HttpAnyMethod("PROPFIND",url);
        return H.getHttpClient().executeMethod(propfind);
    }
    
    @Test
    public void testDefaultWorkspace() throws HttpException, IOException {
        assertEquals(207, getPropfindStatus("default", testPath));
    }
    
    @Test
    public void testEmptyWorkspace() throws HttpException, IOException {
        // An empty JCR workspace name results in a URL like "/dav//test/..."
        // which correctly returns 404 now, but used to work as the WebDAV
        // servlets used the default workspace name in that case.
        assertEquals(404, getPropfindStatus("", testPath));
    }
}
