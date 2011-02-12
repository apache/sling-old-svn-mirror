package org.apache.sling.samples.dependenciesinstall;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Session;

import junit.framework.Assert;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SlingAnnotationsTestRunner.class)
public class MultiValuedResourceTypeTest {

    @TestReference
    private ResourceResolverFactory resourceResolverFactory;

    private String testPath;

    private ResourceResolver resourceResolver;

    @Before
    public void setup() throws Exception {
        resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

        Session session = resourceResolver.adaptTo(Session.class);
        testPath = "/test_" + new Date().getTime();
        Node testNode = session.getRootNode().addNode(testPath.substring(1), "nt:unstructured");
        testNode.setProperty("sling:resourceType", new String[] { "foo/bar", "bar/foo" });
        session.save();
    }
    
    @Test
    public void test() {
        Resource res = resourceResolver.resolve(testPath);
        Assert.assertEquals("foo/bar", res.getResourceType());
    }

    @After
    public void teardown() throws Exception {
        Session session = resourceResolver.adaptTo(Session.class);
        Node testNode = session.getNode(testPath);
        testNode.remove();
        session.save();
    }

}
