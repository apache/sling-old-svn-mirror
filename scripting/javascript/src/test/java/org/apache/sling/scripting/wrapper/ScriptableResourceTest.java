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
package org.apache.sling.scripting.wrapper;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.RepositoryScriptingTestBase;
import org.apache.sling.scripting.javascript.internal.ScriptEngineHelper;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

public class ScriptableResourceTest extends RepositoryScriptingTestBase {

    private Node node;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        node = getNewNode();
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
        assertEquals(Undefined.instance,
            script.eval("resource.adaptTo()", data));
        assertEquals(Undefined.instance, script.eval("resource.adaptTo(null)",
            data));
        assertEquals(Undefined.instance, script.eval(
            "resource.adaptTo(undefined)", data));
        assertEquals(Undefined.instance, script.eval(
            "resource.adaptTo(Packages.java.util.Date)", data));
    }
    
    public void testResourceWrapperClass() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("resource", new TestResource(node));
        
        assertEquals(
                "org.apache.sling.scripting.javascript.wrapper.ScriptableResource", 
                script.eval("resource.javascriptWrapperClass.getName()", data)
        );
    }

    private void assertEquals(Node expected, Object actual) {
        while (actual instanceof Wrapper) {
            actual = ((Wrapper) actual).unwrap();
        }

        super.assertEquals(expected, actual);
    }

    private static class TestResource implements Resource {

        private final Node node;

        private final String path;

        private final String resourceType;

        private final ResourceMetadata metadata;

        TestResource(Node node) throws RepositoryException {
            this.node = node;
            this.path = node.getPath();
            this.resourceType = node.getPrimaryNodeType().getName();
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
            // none, don't care
            return null;
        }

        public String getResourceType() {
            return resourceType;
        }

        public String getResourceSuperType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if (type == Node.class || type == Item.class) {
                return (AdapterType) node;
            }

            return null;
        }

    }

}
