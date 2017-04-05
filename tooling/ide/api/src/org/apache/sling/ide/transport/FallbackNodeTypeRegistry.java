/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.transport;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jcr.nodetype.NodeType;

/**
 * The <tt>FallbackNodeTypeRegistry</tt> holds a conservative, static, registry of node types
 *
 */
public class FallbackNodeTypeRegistry implements NodeTypeRegistry {

    public static FallbackNodeTypeRegistry createRegistryWithDefaultNodeTypes() {

        final FallbackNodeTypeRegistry registry = new FallbackNodeTypeRegistry();

        registry.addNodeType("nt:file", new String[] { "nt:hierarchyNode" });
        registry.addNodeType("nt:folder", new String[] { "nt:hierarchyNode" });
        registry.addNodeType("nt:hierarchyNode", new String[] { "mix:created", "nt:base" });
        registry.addNodeType("nt:unstructured", new String[] { "nt:base" });
        registry.addNodeType("nt:base", new String[] {});
        registry.addNodeType("sling:OsgiConfig", new String[] { "nt:hierarchyNode", "nt:unstructured" });
        registry.addNodeType("sling:Folder", new String[] { "nt:folder" });
        registry.addNodeType("sling:OrderedFolder", new String[] { "sling:Folder" });
        registry.addNodeType("vlt:FullCoverage", new String[] {});
        registry.addNodeType("mix:created", new String[] {});

        return registry;

    }

    private final List<NodeType> nodeTypes = new ArrayList<>();

    @Override
    public boolean isAllowedPrimaryChildNodeType(String parentNodeType, String childNodeType)
            throws RepositoryException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Collection<String> getAllowedPrimaryChildNodeTypes(String parentNodeType) throws RepositoryException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public void addNodeType(String name, String[] superTypeNames) {

        nodeTypes.add(getNodeTypeProxy(name, superTypeNames));
    }

    private NodeType getNodeTypeProxy(final String name, final String[] superTypeNames) {

        InvocationHandler ih = new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                // NodeType.getName()
                if (method.getName().equals("getName") && method.getParameterTypes().length == 0) {
                    return name;
                }

                // NodeType.getSupertypeNames();
                if (method.getName().equals("getDeclaredSupertypeNames") && method.getParameterTypes().length == 0) {
                    return superTypeNames;
                }

                // NodeType.getDeclaredSupertypeNames();
                if (method.getName().equals("getSupertypes") && method.getParameterTypes().length == 0) {
                    NodeType[] superTypes = new NodeType[superTypeNames.length];
                    for (int i = 0; i < superTypeNames.length; i++) {
                        String aSuperTypeName = superTypeNames[i];
                        NodeType aSuperType = getNodeType(aSuperTypeName);
                        superTypes[i] = aSuperType;
                    }

                    return superTypes;
                }

                // Object.toString() , Object.hashCode(), Object.equals
                if (method.getDeclaringClass() == Object.class) {
                    if (method.getName().equals("toString") && method.getParameterTypes().length == 0) {
                        return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                    }

                    if (method.getName().equals("hashCode") && method.getParameterTypes().length == 0) {
                        return System.identityHashCode(proxy);
                    }

                    if (method.getName().equals("equals") && method.getParameterTypes().length == 1) {
                        return proxy == args[0];
                    }
                }

                return null;
            }
        };

        NodeType nodeType = (NodeType) Proxy.newProxyInstance(NodeType.class.getClassLoader(),
                new Class[] { NodeType.class }, ih);

        return nodeType;
    }

    @Override
    public List<NodeType> getNodeTypes() {
        return Collections.unmodifiableList(nodeTypes);
    }

    @Override
    public NodeType getNodeType(String name) {

        for (NodeType nt : nodeTypes) {
            if (nt.getName().equals(name)) {
                return nt;
            }
        }

        return null;
    }
}
