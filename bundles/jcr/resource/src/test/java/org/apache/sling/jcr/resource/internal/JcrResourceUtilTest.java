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
package org.apache.sling.jcr.resource.internal;

import javax.jcr.Node;

import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.jcr.resource.JcrResourceUtil;

/**
 * Test of JcrResourceUtil.
 */
public class JcrResourceUtilTest extends RepositoryTestBase {
    private String rootPath;

    private Node rootNode;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        rootPath = "/test" + System.currentTimeMillis();
        rootNode = getSession().getRootNode().addNode(rootPath.substring(1),
                "nt:unstructured");
        session.save();
    }

    @Override
    protected void tearDown() throws Exception {
        if (rootNode != null) {
            rootNode.remove();
            session.save();
        }

        super.tearDown();
    }

    public void testCreatePathRootExists() throws Exception {
        String pathToCreate = rootPath + "/a/b/c";
        Node created = JcrResourceUtil.createPath(pathToCreate, "nt:unstructured", "nt:unstructured", session, true);
        assertNotNull(created);
        assertEquals(created.getPath(), pathToCreate);

        //test appending the path
        pathToCreate = pathToCreate + "/d";
        created = JcrResourceUtil.createPath(pathToCreate, "nt:unstructured", "nt:unstructured", session, true);

        assertNotNull(created);
        assertEquals(created.getPath(), pathToCreate);
        assertTrue(session.itemExists(pathToCreate));
    }

    public void testCreateRoot() throws Exception {
        String pathToCreate = "/";
        Node created = JcrResourceUtil.createPath(pathToCreate, "nt:unstructured", "nt:unstructured", session, true);

        assertNotNull(created);

        assertEquals(created.getPath(), pathToCreate);
    }


    public void testCreatePathThatExists() throws Exception {
        String pathToCreate = rootPath + "/a/b/c";
        Node created = JcrResourceUtil.createPath(pathToCreate, "nt:unstructured", "nt:unstructured", session, true);
        assertNotNull(created);
        assertEquals(created.getPath(), pathToCreate);

        created = JcrResourceUtil.createPath(pathToCreate, "nt:unstructured", "nt:unstructured", session, true);

        assertNotNull(created);
        assertEquals(created.getPath(), pathToCreate);
        assertTrue(session.itemExists(pathToCreate));
    }

    public void testCreatePathBoundaryCase() throws Exception {
        String pathToCreate = "/a";
        Node created = JcrResourceUtil.createPath(pathToCreate, "nt:unstructured", "nt:unstructured", session, true);
        assertNotNull(created);
        assertEquals(created.getPath(), pathToCreate);
        assertTrue(session.itemExists(pathToCreate));
    }

    public void testCreatePathNodeVariant() throws Exception {
        String pathToCreate = rootPath + "/a/b/c";
        Node created = JcrResourceUtil.createPath(pathToCreate, "nt:unstructured", "nt:unstructured", session, true);
        assertNotNull(created);
        assertEquals(created.getPath(), pathToCreate);

        //test appending the path
        created = JcrResourceUtil.createPath(created, "d/e", "nt:unstructured", "nt:unstructured", true);

        pathToCreate =  pathToCreate + "/d/e";
        assertNotNull(created);
        assertEquals(created.getPath(), pathToCreate);
        assertTrue(session.itemExists(pathToCreate));
    }
}
