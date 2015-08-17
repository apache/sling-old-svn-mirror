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
package org.apache.sling.scripting.javascript.wrapper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.jcr.Item;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.scripting.javascript.RepositoryScriptingTestBase;
import org.apache.sling.scripting.javascript.internal.ScriptEngineHelper;
import org.mozilla.javascript.Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptableResourceTest extends RepositoryScriptingTestBase {

    private Node node;

    private static final ResourceResolver RESOURCE_RESOLVER = new MockResourceResolver();

    private static final String RESOURCE_TYPE = "testWrappedResourceType";

    private static final String RESOURCE_SUPER_TYPE = "testWrappedResourceSuperType";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptableResourceTest.class);

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        node = getNewNode();

        try {
            node.getSession().getWorkspace().getNamespaceRegistry().registerNamespace(
                SlingConstants.NAMESPACE_PREFIX,
                JcrResourceConstants.SLING_NAMESPACE_URI);
        } catch (NamespaceException ne) {
            // don't care, might happen if already registered
        }
    }

    public void testDefaultValuePath() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));

        // the path of the resource
        assertEquals(node.getPath(), script.evalToString("out.print(resource)",
            data));
        assertEquals(node.getPath(), script.eval("resource.path", data));
        assertEquals(node.getPath(), script.eval("resource.getPath()", data));
    }

    public void testResourceType() throws Exception {
        // set resource and resource super type
        node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            RESOURCE_TYPE);
        node.setProperty(
            JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY,
            RESOURCE_SUPER_TYPE);

        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));

        // the resourceType of the resource
        assertEquals(RESOURCE_TYPE, script.eval("resource.type", data));
        assertEquals(RESOURCE_TYPE, script.eval("resource.resourceType", data));
        assertEquals(RESOURCE_TYPE, script.eval("resource.getResourceType()",
            data));
    }

    public void testChildren() throws Exception {
        node.addNode("first-child");
        node.addNode("second-child");

        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));

        assertEquals(2.0, script.eval("resource.getChildren().length", data));
        assertEquals("first-child", script.eval("resource.getChildren()[0].name", data));
    }

    public void testListChildren() throws Exception {
        Node firstChild = node.addNode("first-child");
        node.addNode("second-child");

        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));

        assertEquals(2.0, script.eval("resource.listChildren().length", data));
        assertEquals(firstChild.getPath(), script.eval("resource.listChildren()[0].path", data));
    }

    public void testGetChild() throws Exception {
        Node child = node.addNode("child");

        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));

        assertEquals(child.getPath(), script.eval("resource.getChild('./child').path", data));
    }

    public void testGetParent() throws Exception {
        Node child = node.addNode("child");
        Node grandChild = child.addNode("grandchild");

        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(grandChild));

        assertEquals(child.getPath(), script.eval("resource.getParent().getPath()", data));
    }

    public void testParent() throws Exception {
        Node child = node.addNode("child");
        Node grandChild = child.addNode("grandchild");

        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(grandChild));

        assertEquals(child.getPath(), script.eval("resource.parent.path", data));
    }

    public void testIsResourceType() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));
        assertEquals(Boolean.TRUE, script.eval("resource.isResourceType('" + RESOURCE_TYPE + "')", data));
    }

    public void testResourceSuperType() throws Exception {
        // set resource and resource super type
        node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            RESOURCE_TYPE);
        node.setProperty(
            JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY,
            RESOURCE_SUPER_TYPE);

        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));

        // the resourceType of the resource
        assertEquals(RESOURCE_SUPER_TYPE, script.eval(
            "resource.resourceSuperType", data));
        assertEquals(RESOURCE_SUPER_TYPE, script.eval(
            "resource.getResourceSuperType()", data));
    }

    public void testResourceMetadata() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));

        // official API
        assertResourceMetaData(script.eval("resource.resourceMetadata", data));
        assertResourceMetaData(script.eval("resource.getResourceMetadata()",
            data));

        // deprecated mappings
        assertResourceMetaData(script.eval("resource.meta", data));
        assertResourceMetaData(script.eval("resource.getMetadata()", data));
    }

    public void testResourceResolver() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));

        // official API
        assertEquals(RESOURCE_RESOLVER, script.eval(
            "resource.resourceResolver", data));
        assertEquals(RESOURCE_RESOLVER, script.eval(
            "resource.getResourceResolver()", data));
    }

    public void testAdaptToNode() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));

        // the node to which the resource adapts
        assertEquals(node, script.eval("resource.adaptTo('javax.jcr.Node')",
            data));
        assertEquals(node, script.eval(
            "resource.adaptTo(Packages.javax.jcr.Node)", data));
    }

    public void testAdaptToNothing() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));

        // the node to which the resource adapts
        assertEquals(true, script.eval("resource.adaptTo() == undefined", data));
        assertEquals(true, script.eval("resource.adaptTo(null) == undefined", data));
        assertEquals(true, script.eval("resource.adaptTo(undefined) == undefined", data));
        assertEquals(true, script.eval("resource.adaptTo(Packages.java.util.Date) == undefined", data));
    }

    public void testProperties() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        Calendar date = new GregorianCalendar();
        node.setProperty(JcrConstants.JCR_LASTMODIFIED, date);
        node.setProperty("test", "testProperties");
        node.getSession().save();
        data.put("resource", new TestResource(node));
        assertEquals(date.getTimeInMillis(), script.eval("(resource.properties['jcr:lastModified']).getTimeInMillis()", data));
        assertEquals("testProperties", script.eval("resource.properties.test", data));
    }

    private void assertEquals(Node expected, Object actual) {
        while (actual instanceof Wrapper) {
            actual = ((Wrapper) actual).unwrap();
        }

        super.assertEquals(expected, actual);
    }

    private void assertResourceMetaData(Object metaData) throws Exception {
        if (metaData instanceof ResourceMetadata) {
            ResourceMetadata rm = (ResourceMetadata) metaData;
            rm.getResolutionPath().equals(node.getPath());
        } else {
            fail("Expected ResourceMetadata, got " + metaData);
        }
    }

    private static class TestResource implements Resource {

        private final Node node;

        private final String path;

        private final ResourceMetadata metadata;

        TestResource(Node node) throws RepositoryException {
            this.node = node;
            this.path = node.getPath();
            this.metadata = new ResourceMetadata();
            this.metadata.setResolutionPath(this.path);
        }

        public String getPath() {
            return path;
        }

        public ResourceMetadata getResourceMetadata() {
            return metadata;
        }

        public ResourceResolver getResourceResolver() {
            return RESOURCE_RESOLVER;
        }

        public String getResourceType() {
            return RESOURCE_TYPE;
        }

        public String getResourceSuperType() {
            return RESOURCE_SUPER_TYPE;
        }

        @SuppressWarnings("unchecked")
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if (type == Node.class || type == Item.class) {
                return (AdapterType) node;
            } else if (type == ValueMap.class) {
                try {

                    PropertyIterator iterator = node.getProperties();
                    Map<String, Object> properties = new HashMap<String, Object>();
                    while (iterator.hasNext()) {
                        Property prop = iterator.nextProperty();
                        if (prop.isMultiple()) {
                            Value[] values = prop.getValues();
                            Object[] array = new Object[values.length];
                            int index = 0;
                            for (Value value : values) {
                                switch (value.getType()) {
                                    case PropertyType.BINARY:
                                        array[index++] = value.getBinary();
                                        break;
                                    case PropertyType.BOOLEAN:
                                        array[index++] = value.getBoolean();
                                        break;
                                    case PropertyType.DATE:
                                        array[index++] = value.getDate();
                                        break;
                                    case PropertyType.DECIMAL:
                                        array[index++] = value.getDecimal();
                                        break;
                                    case PropertyType.DOUBLE:
                                        array[index++] = value.getDouble();
                                        break;
                                    case PropertyType.LONG:
                                        array[index++] = value.getLong();
                                        break;
                                    default:
                                        array[index++] = value.getString();
                                        break;
                                }
                            }
                            properties.put(prop.getName(), array);

                        } else {
                            Value value = prop.getValue();
                            switch (value.getType()) {
                                case PropertyType.BINARY:
                                    properties.put(prop.getName(), value.getBinary());
                                    break;
                                case PropertyType.BOOLEAN:
                                    properties.put(prop.getName(), value.getBoolean());
                                    break;
                                case PropertyType.DATE:
                                    properties.put(prop.getName(), value.getDate());
                                    break;
                                case PropertyType.DECIMAL:
                                    properties.put(prop.getName(), value.getDecimal());
                                    break;
                                case PropertyType.DOUBLE:
                                    properties.put(prop.getName(), value.getDouble());
                                    break;
                                case PropertyType.LONG:
                                    properties.put(prop.getName(), value.getLong());
                                    break;
                                default:
                                    properties.put(prop.getName(), value.getString());
                                    break;
                            }
                        }
                    }
                    return ((AdapterType) (new ValueMapDecorator(properties)));
                } catch (RepositoryException e) {
                    LOGGER.error("Unable to adapt resource " + getPath() + " to a ValueMap.", e);
                }
            }

            return null;
        }

        public String getName() {
            return ResourceUtil.getName(getPath());
        }

        public Resource getChild(String relPath) {
            try
            {
                Node childNode = node.getNode( relPath );
                if ( childNode !=  null ) {
                    return new TestResource( childNode );
                } else {
                    return null;
                }
            } catch ( RepositoryException re )
            {
                return null;
            }
        }

        public Resource getParent() {
            try
            {
                Node parentNode = node.getParent();
                if ( parentNode !=  null ) {
                    return new TestResource( parentNode );
                } else {
                    return null;
                }
            } catch ( RepositoryException re )
            {
                return null;
            }
        }

        public boolean isResourceType(String resourceType) {
            return getResourceType().equals( resourceType );
        }

        public Iterator<Resource> listChildren() {
            try
            {
                List<Resource> childList = new ArrayList<Resource>();
                NodeIterator it = node.getNodes();
                while ( it.hasNext() )
                {
                    Node nextNode = it.nextNode();
                    childList.add( new TestResource( nextNode ) );
                }
                return childList.iterator();
            } catch ( RepositoryException re )
            {
                return null;
            }
        }


    }

}
