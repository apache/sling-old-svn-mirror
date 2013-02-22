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
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.jcr.resource.JcrResourceConstants;

public class JcrNodeResourceTest extends JcrItemResourceTestBase {

    public void testNtFileNtResource() throws Exception {

        String name = "file";
        Node file = rootNode.addNode(name, JcrConstants.NT_FILE);
        Node res = file.addNode(JcrConstants.JCR_CONTENT,
            JcrConstants.NT_RESOURCE);
        setupResource(res);
        getSession().save();

        file = rootNode.getNode(name);
        JcrNodeResource jnr = new JcrNodeResource(null, file, null);

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
        JcrNodeResource jnr = new JcrNodeResource(null, file, null);

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
        JcrNodeResource jnr = new JcrNodeResource(null, res, null);

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
        JcrNodeResource jnr = new JcrNodeResource(null, res, null);

        assertEquals(res.getPath(), jnr.getPath());

        assertResourceMetaData(jnr.getResourceMetadata());
        assertEquals(TEST_DATA, jnr.adaptTo(InputStream.class));
    }

    public void testResourceType() throws Exception {
        String name = "resourceType";
        Node node = rootNode.addNode(name, JcrConstants.NT_UNSTRUCTURED);
        getSession().save();

        JcrNodeResource jnr = new JcrNodeResource(null, node, null);
        assertEquals(JcrConstants.NT_UNSTRUCTURED, jnr.getResourceType());

        String typeName = "some/resource/type";
        node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, typeName);
        getSession().save();

        jnr = new JcrNodeResource(null, node, null);
        assertEquals(typeName, jnr.getResourceType());
    }

    public void testResourceSuperType() throws Exception {
        String name = "resourceSuperType";
        String typeNodeName = "some_resource_type";
        String typeName = rootPath + "/" + typeNodeName;
        Node node = rootNode.addNode(name, JcrConstants.NT_UNSTRUCTURED);
        node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, typeName);
        getSession().save();

        Resource jnr = new JcrNodeResource(null, node, null);
        assertEquals(typeName, jnr.getResourceType());

        // default super type is null
        assertNull(jnr.getResourceSuperType());

        String superTypeName = "supertype";
        Node typeNode = rootNode.addNode(typeNodeName, JcrConstants.NT_UNSTRUCTURED);
        typeNode.setProperty(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY, superTypeName);
        getSession().save();

        jnr = new JcrNodeResource(null, typeNode, null);
        assertEquals(JcrConstants.NT_UNSTRUCTURED, jnr.getResourceType());
        assertEquals(superTypeName, jnr.getResourceSuperType());

        // overwrite super type with direct property
        String otherSuperTypeName = "othersupertype";
        node.setProperty(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY, otherSuperTypeName);
        getSession().save();

        jnr = new JcrNodeResource(null, node, null);
        assertEquals(typeName, jnr.getResourceType());
        assertEquals(otherSuperTypeName, jnr.getResourceSuperType());

        // remove direct property to get supertype again
        node.getProperty(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY).remove();
        getSession().save();

        jnr = new JcrNodeResource(null, node, null);
        assertEquals(typeName, jnr.getResourceType());
        assertNull(jnr.getResourceSuperType());
    }

    public void testAdaptToMap() throws Exception {

        String name = "adaptable";
        Node res = rootNode.addNode(name, JcrConstants.NT_UNSTRUCTURED);
        setupResource(res);
        getSession().save();

        res = rootNode.getNode(name);
        JcrNodeResource jnr = new JcrNodeResource(null, res, null);

        final Map<?, ?> props = jnr.adaptTo(Map.class);

        // assert we have properties at all, only fails if property
        // retrieval fails for any reason
        assertNotNull(props);
        assertFalse(props.isEmpty());

        // assert all properties set up
        assertEquals(TEST_MODIFIED, props.get(JcrConstants.JCR_LASTMODIFIED));
        assertEquals(TEST_TYPE, props.get(JcrConstants.JCR_MIMETYPE));
        assertEquals(TEST_ENCODING, props.get(JcrConstants.JCR_ENCODING));
        assertEquals(TEST_DATA, (InputStream) props.get(JcrConstants.JCR_DATA));

        // assert JCR managed properties
        assertEquals(JcrConstants.NT_UNSTRUCTURED, props.get(JcrConstants.JCR_PRIMARYTYPE));

        // assert we have nothing else left
        final Set<String> existingKeys = new HashSet<String>();
        existingKeys.add(JcrConstants.JCR_LASTMODIFIED);
        existingKeys.add(JcrConstants.JCR_MIMETYPE);
        existingKeys.add(JcrConstants.JCR_ENCODING);
        existingKeys.add(JcrConstants.JCR_DATA);
        existingKeys.add(JcrConstants.JCR_PRIMARYTYPE);
        final Set<Object> crossCheck = new HashSet<Object>(props.keySet());
        crossCheck.removeAll(existingKeys);
        assertTrue(crossCheck.isEmpty());

        // call a second time, ensure the map contains the same data again
        final Map<?, ?> propsSecond = jnr.adaptTo(Map.class);

        // assert we have properties at all, only fails if property
        // retrieval fails for any reason
        assertNotNull(propsSecond);
        assertFalse(propsSecond.isEmpty());

        // assert all properties set up
        assertEquals(TEST_MODIFIED, propsSecond.get(JcrConstants.JCR_LASTMODIFIED));
        assertEquals(TEST_TYPE, propsSecond.get(JcrConstants.JCR_MIMETYPE));
        assertEquals(TEST_ENCODING, propsSecond.get(JcrConstants.JCR_ENCODING));
        assertEquals(TEST_DATA, (InputStream) propsSecond.get(JcrConstants.JCR_DATA));

        // assert JCR managed properties
        assertEquals(JcrConstants.NT_UNSTRUCTURED, propsSecond.get(JcrConstants.JCR_PRIMARYTYPE));

        // assert we have nothing else left
        final Set<Object> crossCheck2 = new HashSet<Object>(propsSecond.keySet());
        crossCheck2.removeAll(existingKeys);
        assertTrue(crossCheck2.isEmpty());
    }

    public void testCorrectUTF8ByteLength() throws Exception {
        byte[] utf8bytes = "Übersättigung".getBytes("UTF-8");
        String name = "utf8file";
        Node file = rootNode.addNode(name, JcrConstants.NT_FILE);
        Node res = file.addNode(JcrConstants.JCR_CONTENT,
            JcrConstants.NT_RESOURCE);

        res.setProperty(JcrConstants.JCR_LASTMODIFIED, TEST_MODIFIED);
        res.setProperty(JcrConstants.JCR_MIMETYPE, TEST_TYPE);
        res.setProperty(JcrConstants.JCR_ENCODING, "UTF-8");
        res.setProperty(JcrConstants.JCR_DATA, new ByteArrayInputStream(utf8bytes));

        getSession().save();

        file = rootNode.getNode(name);
        JcrNodeResource jnr = new JcrNodeResource(null, file, null);

        assertEquals(utf8bytes, jnr.adaptTo(InputStream.class));
        assertEquals(utf8bytes.length, jnr.getResourceMetadata().getContentLength());
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

        assertEquals(TEST_MODIFIED, rm.getModificationTime());
        assertEquals(TEST_TYPE, rm.getContentType());
        assertEquals(TEST_ENCODING, rm.getCharacterEncoding());
    }
}
