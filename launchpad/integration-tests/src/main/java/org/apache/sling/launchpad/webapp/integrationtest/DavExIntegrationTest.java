/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/**
 * Test of Davex
 */
public class DavExIntegrationTest extends HttpTestBase {

    private Repository repository;
    public static final String DAVEX_SERVER_URL = HTTP_BASE_URL + "/server/"; 

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configureServerBeforeTest();
        repository = JcrUtils.getRepository(DAVEX_SERVER_URL);
    }

    protected void configureServerBeforeTest() throws Exception {
    }

    public void testDescriptor() throws Exception {
        assertEquals("2.0", repository.getDescriptor(Repository.SPEC_VERSION_DESC));
        assertTrue(repository.getDescriptor(Repository.REP_NAME_DESC).contains("Jackrabbit"));
    }

    /** Create a node via Sling's http interface and verify that admin can
     *  read it via davex remote access.
     */
    public void testReadNode() throws Exception {
        final String path = "/DavExNodeTest_1_" + System.currentTimeMillis();
        final String url = HTTP_BASE_URL + path;

        // add some properties to the node
        final Map<String, String> props = new HashMap<String, String>();
        props.put("name1", "value1");
        props.put("name2", "value2");

        testClient.createNode(url, props);

        // Oak does not support login without credentials, so to
        // verify that davex access works we do need valid repository credentials.
        final Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repository.login(creds);

        try {
            final Node node = session.getNode(path);
            assertNotNull(node);
            assertEquals("value1", node.getProperty("name1").getString());
            assertEquals("value2", node.getProperty("name2").getString());
        } finally {
            session.logout();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        repository = null;
        configureServerAfterTest();
        super.tearDown();
    }

    protected void configureServerAfterTest() throws Exception {
    }
}
