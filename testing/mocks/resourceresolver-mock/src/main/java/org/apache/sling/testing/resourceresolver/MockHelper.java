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
package org.apache.sling.testing.resourceresolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

/**
 * Helper class to create resources:
 *
 * MockHelper.create(resolver).resource("/libs").p("prop", "value")
 *                              .resource("sub").p("sub", "hello")
 *                              .resource(".sameLevel")
 *                            .resource("/apps").p("foo", "baa").commit()
 *
 */
public class MockHelper {

    private final ResourceResolver resolver;

    private final Stack<Description> stack = new Stack<Description>();

    private MockHelper(final ResourceResolver r) {
        this.resolver = r;
    }


    /**
     * Create a new helper
     */
    public static MockHelper create(final ResourceResolver resolver) {
        return new MockHelper(resolver);
    }

    /**
     * Add a new resource
     * If the path is relative, this resource is added as a child to the previous resource.
     * If the path is relative and starts with a dot, this resource is added as a peer to
     * the previous resource.
     */
    public MockHelper resource(final String path) {
        final String fullPath;
        if ( path.startsWith("/") ) {
            fullPath = path;
        } else if ( path.startsWith(".") ) {
            final Description d = this.stack.peek();
            fullPath = ResourceUtil.normalize(d.path + "/../" + path.substring(1));
        } else {
            final Description d = this.stack.peek();
            fullPath = d.path + "/" + path;
        }
        final Description d = new Description();
        d.path = fullPath;
        this.stack.push(d);

        return this;
    }

    /**
     * Add a property to the current resource
     */
    public MockHelper p(final String name, final Object value) {
        final Description d = this.stack.peek();
        d.properties.put(name, value);

        return this;
    }

    /**
     * Finish building and add all resources to the resource tree.
     */
    public void add() throws PersistenceException {
        for(int i=0; i<this.stack.size(); i++) {
            final Description d = this.stack.get(i);
            this.create(d.path, d.properties);
        }
        this.stack.clear();
    }

    /**
     * Finish building, add all resources to the resource tree and commit
     * changes.
     * @throws PersistenceException
     */
    public void commit() throws PersistenceException {
        this.add();
        this.resolver.commit();
    }

    private void create(final String path, final Map<String, Object> properties) throws PersistenceException {
        final String parentPath = ResourceUtil.getParent(path);
        final String name = ResourceUtil.getName(path);

        final Resource parent = this.resolver.getResource(parentPath);
        this.resolver.create(parent, name, properties);
    }

    private static final class Description {
        public String path;
        public Map<String, Object> properties = new HashMap<String, Object>();
    }
}
