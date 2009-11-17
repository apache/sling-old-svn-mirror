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
package org.apache.sling.commons.testing.sling;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.adapter.AdapterFactory;

/**
 * <code>SlingTestHelper</code> provides various helper methods for accessing
 * standard sling features in a test environment without the OSGi service framework
 * available. This includes:
 *
 * - register standard sling node types
 * - simple adaptable manager implementation (requires {@link AdapterManagerTestHelper})
 */
public class SlingTestHelper {

    public static void registerSlingNodeTypes(Session adminSession) throws IOException, RepositoryException {
        Class<SlingTestHelper> clazz = SlingTestHelper.class;
        org.apache.sling.commons.testing.jcr.RepositoryUtil.registerNodeType(adminSession,
                clazz.getResourceAsStream("/SLING-INF/nodetypes/folder.cnd"));
        org.apache.sling.commons.testing.jcr.RepositoryUtil.registerNodeType(adminSession,
                clazz.getResourceAsStream("/SLING-INF/nodetypes/resource.cnd"));
        org.apache.sling.commons.testing.jcr.RepositoryUtil.registerNodeType(adminSession,
                clazz.getResourceAsStream("/SLING-INF/nodetypes/vanitypath.cnd"));
    }

    public static void registerAdapterFactory(AdapterFactory adapterFactory,
            String[] adaptableClasses, String[] adapterClasses) {
        AdapterManagerTestHelper.registerAdapterFactory(adapterFactory, adaptableClasses, adapterClasses);
    }

    public static void resetAdapterFactories() {
        AdapterManagerTestHelper.resetAdapterFactories();
    }

    public static void printJCR(Session session) throws RepositoryException {
        printJCR(session.getRootNode(), new String[] {});
    }

    public static void printJCR(Node node) throws RepositoryException {
        printJCR(node, new String[] {});
    }

    public static void printJCR(Session session, String path, String... props) throws RepositoryException {
        printJCR((Node) session.getItem(path), props);
    }

    public static void printJCR(Node node, String... props) throws RepositoryException {
        System.out.println(node.getPath());
        if (props != null) {
            for (String prop: props) {
                if (node.hasProperty(prop)) {
                    Property property = node.getProperty(prop);
                    System.out.println(property.getPath() + " = " + property.getString());
                }
            }
        }
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node child = nodes.nextNode();
            if (!child.getName().equals("jcr:system")) {
                printJCR(child, props);
            }
        }
    }

}
