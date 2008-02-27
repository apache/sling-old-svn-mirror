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
import javax.swing.RootPaneContainer;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
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

    public void testResourceType() throws Exception {
        String name = "resourceType";
        Node node = rootNode.addNode(name, JcrConstants.NT_UNSTRUCTURED);
        getSession().save();
        
        JcrNodeResource jnr = new JcrNodeResource(null, node);
        assertEquals(JcrConstants.NT_UNSTRUCTURED, jnr.getResourceType());
        
        String typeName = "some/resource/type";
        node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, typeName);
        getSession().save();
        
        jnr = new JcrNodeResource(null, node);
        assertEquals(typeName, jnr.getResourceType());
    }
    
    public void testResourceSuperType() throws Exception {
        String name = "resourceSuperType";
        String typeNodeName = "some_resource_type";
        String typeName = rootPath + "/" + typeNodeName;
        Node node = rootNode.addNode(name, JcrConstants.NT_UNSTRUCTURED);
        node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, typeName);
        getSession().save();

        Resource jnr = new JcrNodeResource(resourceResolver, node);
        assertEquals(typeName, jnr.getResourceType());

        // default super type is super type of node type
        assertEquals(null, jnr.getResourceSuperType());

        String superTypeName = "supertype";
        Node typeNode = rootNode.addNode(typeNodeName, JcrConstants.NT_UNSTRUCTURED);
        typeNode.setProperty(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY, superTypeName);
        getSession().save();
        
        jnr = new JcrNodeResource(resourceResolver, node);
        assertEquals(typeName, jnr.getResourceType());
        assertEquals(superTypeName, jnr.getResourceSuperType());

        // overwrite super type with direct property
        String otherSuperTypeName = "othersupertype";
        node.setProperty(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY, otherSuperTypeName);
        getSession().save();
        
        jnr = new JcrNodeResource(resourceResolver, node);
        assertEquals(typeName, jnr.getResourceType());
        assertEquals(otherSuperTypeName, jnr.getResourceSuperType());

        // remove direct property to get supertype again
        node.getProperty(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY).remove();
        getSession().save();

        jnr = new JcrNodeResource(resourceResolver, node);
        assertEquals(typeName, jnr.getResourceType());
        assertEquals(superTypeName, jnr.getResourceSuperType());
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
