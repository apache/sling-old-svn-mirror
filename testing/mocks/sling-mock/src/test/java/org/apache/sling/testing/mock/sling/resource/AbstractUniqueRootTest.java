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
package org.apache.sling.testing.mock.sling.resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractUniqueRootTest {
    
    @Rule
    public SlingContext context = new SlingContext(getResourceResolverType());

    protected abstract ResourceResolverType getResourceResolverType();

    @Test
    public void testContent() {
        String path = context.uniqueRoot().content();
        assertNotNull(context.resourceResolver().getResource(path));
        assertTrue(path.matches("^/content/[^/]+"));
    }

    @Test
    public void testApps() throws Exception {
        String path = context.uniqueRoot().apps();
        assertNotNull(context.resourceResolver().getResource(path));
        assertTrue(path.matches("^/apps/[^/]+"));
    }

    @Test
    public void testLibs() throws Exception {
        String path = context.uniqueRoot().libs();
        assertNotNull(context.resourceResolver().getResource(path));
        assertTrue(path.matches("^/libs/[^/]+"));
    }

}
