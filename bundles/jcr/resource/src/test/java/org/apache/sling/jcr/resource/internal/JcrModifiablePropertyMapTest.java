package org.apache.sling.jcr.resource.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.apache.sling.api.resource.PersistableValueMap;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrResourceUtil;

public class JcrModifiablePropertyMapTest extends JcrPropertyMapTest {

    private String rootPath;

    private Node rootNode;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        rootPath = "/test_" + System.currentTimeMillis();
        rootNode = getSession().getRootNode().addNode(rootPath.substring(1),
            "nt:unstructured");

        final Map<String, Object> values = this.initialSet();
        for(Map.Entry<String, Object> entry : values.entrySet()) {
            JcrResourceUtil.setProperty(rootNode, entry.getKey().toString(), entry.getValue());
        }
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

    private Map<String, Object> initialSet() {
        final Map<String, Object> values = new HashMap<String, Object>();
        values.put("string", "test");
        values.put("long", 1L);
        values.put("bool", Boolean.TRUE);
        return values;
    }

    public void testPut()
    throws IOException {
        final PersistableValueMap pvm = new JcrModifiablePropertyMap(this.rootNode);
        assertContains(pvm, initialSet());
        assertNull(pvm.get("something"));

        // now put two values and check set again
        pvm.put("something", "Another value");
        pvm.put("string", "overwrite");

        final Map<String, Object> currentlyStored = this.initialSet();
        currentlyStored.put("something", "Another value");
        currentlyStored.put("string", "overwrite");
        assertContains(pvm, currentlyStored);

        pvm.save();
        assertContains(pvm, currentlyStored);

        final PersistableValueMap pvm2 = new JcrModifiablePropertyMap(this.rootNode);
        assertContains(pvm2, currentlyStored);
    }

    public void testReset()
    throws IOException {
        final PersistableValueMap pvm = new JcrModifiablePropertyMap(this.rootNode);
        assertContains(pvm, initialSet());
        assertNull(pvm.get("something"));

        // now put two values and check set again
        pvm.put("something", "Another value");
        pvm.put("string", "overwrite");

        final Map<String, Object> currentlyStored = this.initialSet();
        currentlyStored.put("something", "Another value");
        currentlyStored.put("string", "overwrite");
        assertContains(pvm, currentlyStored);

        pvm.reset();
        assertContains(pvm, initialSet());

        final PersistableValueMap pvm2 = new JcrModifiablePropertyMap(this.rootNode);
        assertContains(pvm2, initialSet());
    }

    protected JcrPropertyMap createPropertyMap(final Node node) {
        return new JcrModifiablePropertyMap(node);
    }

    /**
     * Check that the value map contains all supplied values
     */
    private void assertContains(ValueMap map, Map<String, Object> values) {
        for(Map.Entry<String, Object> entry : values.entrySet()) {
            final Object stored = map.get(entry.getKey());
            assertEquals(values.get(entry.getKey()), stored);
        }
    }
}
