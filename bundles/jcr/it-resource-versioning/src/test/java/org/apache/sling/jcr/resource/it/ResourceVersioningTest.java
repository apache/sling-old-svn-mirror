package org.apache.sling.jcr.resource.it;

import static org.junit.Assert.*;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;
import javax.naming.NamingException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResourceVersioningTest {

    private VersionManager versionManager;

    private Node testNode;

    private String testPath;

    private ResourceResolver resolver;

    private Session session;

    @Before
    public void setUp() throws Exception {
        resolver = MockSling.newResourceResolver(ResourceResolverType.JCR_JACKRABBIT);
        session = resolver.adaptTo(Session.class);
        versionManager = session.getWorkspace().getVersionManager();

        testNode = session.getRootNode().addNode("mytest");
        testPath = testNode.getPath();
        testNode.addMixin(JcrConstants.MIX_VERSIONABLE);
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        resolver.close();
    }

    @Test
    public void getVersion() throws RepositoryException, NamingException {
        versionManager.checkout(testPath);
        testNode.setProperty("prop", "testvalue1");
        session.save();
        Version v1 = versionManager.checkin(testPath);

        versionManager.checkout(testPath);
        testNode.setProperty("prop", "testvalue2");
        session.save();
        versionManager.checkin(testPath);

        String path = String.format("%s;v='%s'", testPath, v1.getName());
        Resource resource = resolver.getResource(path);
        String prop = resource.adaptTo(ValueMap.class).get("prop", String.class);
        assertEquals("testvalue1", prop);
    }
}
