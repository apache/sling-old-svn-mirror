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
package org.apache.sling.jcr.resource.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.PersistableValueMap;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrModifiablePropertyMap;
import org.apache.sling.jcr.resource.JcrPropertyMap;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.jcr.resource.ValueMapCacheProvider;

public class JcrModifiablePropertyMapTest extends JcrPropertyMapTest {

    private String rootPath;

    private Node rootNode;

    private ValueMapCacheProvider valueMapCacheProvider;

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
        valueMapCacheProvider = new ValueMapCacheProvider();
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
    throws Exception {
        this.rootNode.getSession().refresh(false);
        final PersistableValueMap pvm = new JcrModifiablePropertyMap(this.rootNode, valueMapCacheProvider);
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

        final PersistableValueMap pvm2 = new JcrModifiablePropertyMap(this.rootNode, valueMapCacheProvider);
        assertContains(pvm2, currentlyStored);
    }

    public void testReset()
    throws Exception {
        this.rootNode.getSession().refresh(false);
        final PersistableValueMap pvm = new JcrModifiablePropertyMap(this.rootNode, valueMapCacheProvider);
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

        final PersistableValueMap pvm2 = new JcrModifiablePropertyMap(this.rootNode, valueMapCacheProvider);
        assertContains(pvm2, initialSet());
    }

    public void testSerializable()
    throws Exception {
        this.rootNode.getSession().refresh(false);
        final PersistableValueMap pvm = new JcrModifiablePropertyMap(this.rootNode, valueMapCacheProvider);
        assertContains(pvm, initialSet());
        assertNull(pvm.get("something"));

        // now put a serializable object
        final List<String> strings = new ArrayList<String>();
        strings.add("a");
        strings.add("b");
        pvm.put("something", strings);

        // check if we get the list again
        @SuppressWarnings("unchecked")
        final List<String> strings2 = (List<String>) pvm.get("something");
        assertEquals(strings, strings2);

        pvm.save();

        final PersistableValueMap pvm2 = new JcrModifiablePropertyMap(this.rootNode, valueMapCacheProvider);
        // check if we get the list again
        @SuppressWarnings("unchecked")
        final List<String> strings3 = (List<String>) pvm2.get("something", Serializable.class);
        assertEquals(strings, strings3);

    }

    public void testExceptions() throws Exception {
        this.rootNode.getSession().refresh(false);
        final PersistableValueMap pvm = new JcrModifiablePropertyMap(this.rootNode, valueMapCacheProvider);
        try {
            pvm.put(null, "something");
            fail("Put with null key");
        } catch (NullPointerException iae) {}
        try {
            pvm.put("something", null);
            fail("Put with null value");
        } catch (NullPointerException iae) {}
        try {
            pvm.put("something", pvm);
            fail("Put with non serializable");
        } catch (IllegalArgumentException iae) {}
    }

    private Set<String> getMixinNodeTypes(final Node node) throws RepositoryException {
        final Set<String> mixinTypes = new HashSet<String>();
        for(final NodeType mixinNodeType : node.getMixinNodeTypes() ) {
            mixinTypes.add(mixinNodeType.getName());
        }
        return mixinTypes;
    }

    public void testMixins() throws Exception {
        this.rootNode.getSession().refresh(false);
        final Node testNode = this.rootNode.addNode("testMixins" + System.currentTimeMillis());
        testNode.getSession().save();
        final PersistableValueMap pvm = new JcrModifiablePropertyMap(testNode, valueMapCacheProvider);

        final String[] types = pvm.get("jcr:mixinTypes", String[].class);
        final Set<String> exNodeTypes = getMixinNodeTypes(testNode);

        assertEquals(exNodeTypes.size(), (types == null ? 0 : types.length));
        if ( types != null ) {
            for(final String name : types) {
                assertTrue(exNodeTypes.contains(name));
            }
            String[] newTypes = new String[types.length + 1];
            System.arraycopy(types, 0, newTypes, 0, types.length);
            newTypes[types.length] = "mix:referenceable";
            pvm.put("jcr:mixinTypes", newTypes);
        } else {
            pvm.put("jcr:mixinTypes", "mix:referenceable");
        }
        pvm.save();

        final Set<String> newNodeTypes = getMixinNodeTypes(testNode);
        assertEquals(newNodeTypes.size(), exNodeTypes.size() + 1);
    }

    private static final String TEST_PATH = "a<a";

    private static final String VALUE = "value";
    private static final String VALUE1 = "value";
    private static final String VALUE2 = "value";
    private static final String VALUE3 = "my title";
    private static final String PROP1 = "-prop";
    private static final String PROP2 = "1prop";
    private static final String PROP3 = "jcr:title";

    public void testNamesReverse() throws Exception {
        this.rootNode.getSession().refresh(false);

        final Node testNode = this.rootNode.addNode("nameTest" + System.currentTimeMillis());
        testNode.getSession().save();
        final PersistableValueMap pvm = new JcrModifiablePropertyMap(testNode, valueMapCacheProvider);
        pvm.put(TEST_PATH, VALUE);
        pvm.put(PROP1, VALUE1);
        pvm.put(PROP2, VALUE2);
        pvm.put(PROP3, VALUE3);
        pvm.save();

        // read with property map
        final ValueMap vm = this.createPropertyMap(testNode);
        assertEquals(VALUE, vm.get(TEST_PATH));
        assertEquals(VALUE1, vm.get(PROP1));
        assertEquals(VALUE2, vm.get(PROP2));
        assertEquals(VALUE3, vm.get(PROP3));

        // read properties
        assertEquals(VALUE, testNode.getProperty(TEST_PATH).getString());
        assertEquals(VALUE1, testNode.getProperty(PROP1).getString());
        assertEquals(VALUE2, testNode.getProperty(PROP2).getString());
        assertEquals(VALUE3, testNode.getProperty(PROP3).getString());
    }

    /**
     * Checks property names encoding, see SLING-2502.
     */
    public void testNamesUpdate() throws Exception {
        this.rootNode.getSession().refresh(false);

        final Node testNode = this.rootNode.addNode("nameUpdateTest"
                + System.currentTimeMillis());
        testNode.setProperty(PROP3, VALUE);
        testNode.getSession().save();

        final PersistableValueMap pvm = new JcrModifiablePropertyMap(testNode, valueMapCacheProvider);
        pvm.put(PROP3, VALUE3);
        pvm.put("jcr:a:b", VALUE3);
        pvm.put("jcr:", VALUE3);
        pvm.save();

        // read with property map
        final ValueMap vm = this.createPropertyMap(testNode);
        assertEquals(VALUE3, vm.get(PROP3));
        assertEquals(VALUE3, vm.get("jcr:a:b"));
        assertEquals(VALUE3, vm.get("jcr:"));

        // read properties
        assertEquals(VALUE3, testNode.getProperty(PROP3).getString());
        assertEquals(VALUE3, testNode.getProperty("jcr:"+Text.escapeIllegalJcrChars("a:b")).getString());
        assertEquals(VALUE3, testNode.getProperty(Text.escapeIllegalJcrChars("jcr:")).getString());
        assertFalse(testNode.hasProperty(Text.escapeIllegalJcrChars(PROP3)));
        assertFalse(testNode.hasProperty(Text.escapeIllegalJcrChars("jcr:a:b")));
    }

    protected JcrPropertyMap createPropertyMap(final Node node) {
        return new JcrModifiablePropertyMap(node, valueMapCacheProvider);
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