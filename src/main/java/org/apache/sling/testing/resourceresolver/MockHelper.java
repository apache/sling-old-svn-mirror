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
 *                            .resource("/apps").p("foo", "baa").commit()
 *
 */
public class MockHelper {

    private final ResourceResolver resolver;

    private final Stack<Description> stack = new Stack<Description>();

    private MockHelper(final ResourceResolver r) {
        this.resolver = r;
    }

    public static MockHelper create(final ResourceResolver resolver) {
        return new MockHelper(resolver);
    }

    public MockHelper resource(final String path) {
        final String fullPath;
        if ( !path.startsWith("/") ) {
            final Description d = this.stack.peek();
            fullPath = d.path + "/" + path;
        } else {
            fullPath = path;
        }
        final Description d = new Description();
        d.path = fullPath;
        this.stack.push(d);

        return this;
    }

    public MockHelper p(final String name, final Object value) {
        final Description d = this.stack.peek();
        d.properties.put(name, value);

        return this;
    }

    public void add() throws PersistenceException {
        for(int i=0; i<this.stack.size(); i++) {
            final Description d = this.stack.get(i);
            this.create(d.path, d.properties);
        }
        this.stack.clear();
    }

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
