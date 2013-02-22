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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.jcr.resource.JcrResourceConstants;

public class JcrItemResourceTestBase extends RepositoryTestBase {

    protected static final long TEST_MODIFIED = System.currentTimeMillis();

    protected static final String TEST_TYPE = "text/gurk";

    protected static final String TEST_ENCODING = "ISO-8859-1";

    protected static final byte[] TEST_DATA = { 'S', 'o', 'm', 'e', ' ', 'T',
        'e', 's', 't' };

    protected String rootPath;

    protected Node rootNode;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create the session
        getSession();

        try {
            NamespaceRegistry nsr = session.getWorkspace().getNamespaceRegistry();
            nsr.registerNamespace(SlingConstants.NAMESPACE_PREFIX,
                JcrResourceConstants.SLING_NAMESPACE_URI);
        } catch (Exception e) {
            // don't care for now
        }

        rootPath = "/test" + System.currentTimeMillis();
        rootNode = getSession().getRootNode().addNode(rootPath.substring(1),
            "nt:unstructured");
        getSession().save();
    }

    @Override
    protected void tearDown() throws Exception {
        if (rootNode != null) {
            rootNode.remove();
            getSession().save();
        }

        super.tearDown();
    }

    protected void assertEquals(byte[] expected, InputStream actual)
            throws IOException {
        if (actual == null) {
            fail("Resource stream is null");
        } else {
            try {
                for (byte b : expected) {
                    assertEquals(b, (byte)actual.read());
                }
            } finally {
                try {
                    actual.close();
                } catch (IOException grok) {
                }
            }
        }
    }
}
