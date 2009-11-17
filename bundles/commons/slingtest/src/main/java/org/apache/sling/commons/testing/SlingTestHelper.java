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
package org.apache.sling.commons.testing;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.sling.AdapterManagerTestHelper;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl;
import org.apache.sling.jcr.resource.internal.helper.Mapping;

/**
 * <code>SlingTestHelper</code> provides various helper methods for accessing
 * standard sling features in a test environment without the OSGi service framework
 * available. This includes:
 * 
 * - access to a {@link JcrResourceResolverFactory} and a jcr-based {@link ResourceResolver}
 * - register standard sling node types
 * - simple adaptable manager implementation (requires {@link AdapterManagerTestHelper})
 */
public class SlingTestHelper {
    
    public static JcrResourceResolverFactory 
            getJcrResourceResolverFactory(SlingRepository repository) throws Exception {
        
        JcrResourceResolverFactoryImpl resFac = new JcrResourceResolverFactoryImpl();

        // set all fields that are normally resolved by OSGi SCR via reflection
        
        Field repoField = resFac.getClass().getDeclaredField("repository");
        repoField.setAccessible(true);
        repoField.set(resFac, repository);

        Field mappingsField = resFac.getClass().getDeclaredField("mappings");
        mappingsField.setAccessible(true);
        mappingsField.set(resFac, new Mapping[] { Mapping.DIRECT });

        Field searchPathField = resFac.getClass().getDeclaredField("searchPath");
        searchPathField.setAccessible(true);
        searchPathField.set(resFac, new String[] { "/apps", "/libs" });

        return resFac;
    }

    public static ResourceResolver getResourceResolver(SlingRepository repository, Session session) throws Exception {
        JcrResourceResolverFactory factory = getJcrResourceResolverFactory(repository);
        return factory.getResourceResolver(session);
    }
    
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
