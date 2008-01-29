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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.jcr.resource.JcrResourceConstants;

public class JcrNodeResourceTest extends RepositoryTestBase {

    private static final long TEST_MODIFIED = System.currentTimeMillis();

    private static final String TEST_TYPE = "text/gurk";

    private static final String TEST_ENCODING = "ISO-8859-1";

    private static final byte[] TEST_DATA = { 'S', 'o', 'm', 'e', ' ', 'T',
        'e', 's', 't' };

    private String rootPath;

    private Node rootNode;

    protected void setUp() throws Exception {
        super.setUp();
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
    }

    public void testNtFileNtResource() throws Exception {

        String name = "file";
        Node file = rootNode.addNode(name, JcrConstants.NT_FILE);
        Node res = file.addNode(JcrConstants.JCR_CONTENT,
            JcrConstants.NT_RESOURCE);
        setupResource(res);
        getSession().save();

        file = rootNode.getNode(name);
        JcrNodeResource jnr = new JcrNodeResource(null, file);

        assertEquals(file.getPath(), jnr.getPath());

        assertResourceMetaData(jnr.getResourceMetadata());
        assertEquals(TEST_DATA, jnr.adaptTo(InputStream.class));
    }

    public void testNtFileNtUnstructured() throws Exception {

        String name = "fileunstructured";
        Node file = rootNode.addNode(name, JcrConstants.NT_FILE);
        Node res = file.addNode(JcrConstants.JCR_CONTENT,
            JcrConstants.NT_UNSTRUCTURED);
        setupResource(res);
        getSession().save();

        file = rootNode.getNode(name);
        JcrNodeResource jnr = new JcrNodeResource(null, file);

        assertEquals(file.getPath(), jnr.getPath());

        assertResourceMetaData(jnr.getResourceMetadata());
        assertEquals(TEST_DATA, jnr.adaptTo(InputStream.class));
    }

    public void testNtResource() throws Exception {

        String name = "resource";
        Node res = rootNode.addNode(name, JcrConstants.NT_RESOURCE);
        setupResource(res);
        getSession().save();

        res = rootNode.getNode(name);
        JcrNodeResource jnr = new JcrNodeResource(null, res);

        assertEquals(res.getPath(), jnr.getPath());

        assertResourceMetaData(jnr.getResourceMetadata());
        assertEquals(TEST_DATA, jnr.adaptTo(InputStream.class));
    }

    public void testNtUnstructured() throws Exception {

        String name = "unstructured";
        Node res = rootNode.addNode(name, JcrConstants.NT_UNSTRUCTURED);
        setupResource(res);
        getSession().save();

        res = rootNode.getNode(name);
        JcrNodeResource jnr = new JcrNodeResource(null, res);

        assertEquals(res.getPath(), jnr.getPath());

        assertResourceMetaData(jnr.getResourceMetadata());
        assertEquals(TEST_DATA, jnr.adaptTo(InputStream.class));
    }

    private void setupResource(Node res) throws RepositoryException {
        res.setProperty(JcrConstants.JCR_LASTMODIFIED, TEST_MODIFIED);
        res.setProperty(JcrConstants.JCR_MIMETYPE, TEST_TYPE);
        res.setProperty(JcrConstants.JCR_ENCODING, TEST_ENCODING);
        res.setProperty(JcrConstants.JCR_DATA, new ByteArrayInputStream(
            TEST_DATA));
    }

    private void assertResourceMetaData(ResourceMetadata rm) {
        assertNotNull(rm);

        assertEquals(new Long(TEST_MODIFIED),
            rm.get(ResourceMetadata.MODIFICATION_TIME));
        assertEquals(TEST_TYPE, rm.get(ResourceMetadata.CONTENT_TYPE));
        assertEquals(TEST_ENCODING, rm.get(ResourceMetadata.CHARACTER_ENCODING));
    }

    private void assertEquals(byte[] expected, InputStream actual)
            throws IOException {
        if (actual == null) {
            fail("Rsource stream is null");
        } else {
            try {
                for (byte b : expected) {
                    assertEquals(b, actual.read());
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
