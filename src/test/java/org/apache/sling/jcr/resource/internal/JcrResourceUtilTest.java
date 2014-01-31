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
