package org.apache.sling.commons.json.jcr;

import junit.framework.TestCase;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.testing.jcr.MockNode;

import javax.jcr.RepositoryException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author vidar@idium.no
 * @since Apr 17, 2009 6:57:04 PM
 */
public class JsonJcrNodeTest extends TestCase {

    public void testJcrJsonObject() throws RepositoryException, JSONException {
        MockNode node = new MockNode("/node1");
        node.setProperty("prop1", "value1");
        node.setProperty("prop2", "value2");
        Set<String> ignoredProperties = new HashSet<String>();
        ignoredProperties.add("prop2");
        JsonJcrNode json = new JsonJcrNode(node, ignoredProperties);
        assertTrue("Did not create property", json.has("prop1"));
        assertFalse("Created ignored property", json.has("prop2"));
        assertTrue("Did not create jcr:name", json.has("jcr:name"));
        assertTrue("Did not create jcr:path", json.has("jcr:path"));
    }


}
