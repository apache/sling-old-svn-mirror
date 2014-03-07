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
package org.apache.sling.servlets.resolver.internal.helper;

import static org.apache.sling.servlets.resolver.internal.ServletResolverConstants.DEFAULT_SERVLET_NAME;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.testing.sling.MockResource;

public class LocationIteratorTest extends HelperTestBase {

    private LocationIterator getLocationIterator(final String resourceType,
            final String resourceSuperType) {
       return this.getLocationIterator(resourceType, resourceSuperType, DEFAULT_SERVLET_NAME);
    }

    private LocationIterator getLocationIterator(final String resourceType,
            final String resourceSuperType,
            final String baseResourceType) {
        final LocationIterator li = new LocationIterator(resourceType,
                resourceSuperType,
                baseResourceType,
                this.resourceResolver);
        return li;
    }

    public void testSearchPathEmpty() {
        // expect path gets { "/" }
        resourceResolver.setSearchPath((String[]) null);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /foo/bar
        assertTrue(li.hasNext());
        assertEquals("/" + resourceTypePath, li.next());

        // 2. /sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals("/" + DEFAULT_SERVLET_NAME, li.next());

        // 3. finished
        assertFalse(li.hasNext());
    }

    public void testSearchPath1Element() {
        String root0 = "/apps";
        resourceResolver.setSearchPath(root0);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /apps/foo/bar
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + resourceTypePath, li.next());

        // 2. /apps/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 3. finished
        assertFalse(li.hasNext());
    }

    public void testSearchPath2Elements() {
        String root0 = "/apps";
        String root1 = "/libs";
        resourceResolver.setSearchPath(root0, root1);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /apps/foo/bar
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + resourceTypePath, li.next());

        // 2. /libs/foo/bar
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + resourceTypePath, li.next());

        // 3. /apps/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 4. /libs/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 5. finished
        assertFalse(li.hasNext());
    }

    public void testSearchPathEmptyAbsoluteType() {
        // expect path gets { "/" }
        resourceResolver.setSearchPath((String[]) null);

        // absolute resource type
        resourceType = "/foo/bar";
        resourceTypePath = ResourceUtil.resourceTypeToPath(resourceType);
        resource.setResourceType(resourceType);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /foo/bar
        assertTrue(li.hasNext());
        assertEquals(resourceTypePath, li.next());

        // 2. /sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals("/" + DEFAULT_SERVLET_NAME, li.next());

        // 3. finished
        assertFalse(li.hasNext());
    }

    public void testSearchPath1ElementAbsoluteType() {
        String root0 = "/apps";
        resourceResolver.setSearchPath(root0);

        // absolute resource type
        resourceType = "/foo/bar";
        resourceTypePath = ResourceUtil.resourceTypeToPath(resourceType);
        resource.setResourceType(resourceType);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /foo/bar
        assertTrue(li.hasNext());
        assertEquals(resourceTypePath, li.next());

        // 2. /apps/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 3. finished
        assertFalse(li.hasNext());
    }

    public void testSearchPath2ElementsAbsoluteType() {
        String root0 = "/apps";
        String root1 = "/libs";
        resourceResolver.setSearchPath(root0, root1);

        // absolute resource type
        resourceType = "/foo/bar";
        resourceTypePath = ResourceUtil.resourceTypeToPath(resourceType);
        resource.setResourceType(resourceType);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /foo/bar
        assertTrue(li.hasNext());
        assertEquals(resourceTypePath, li.next());

        // 2. /apps/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 3. /libs/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 4. finished
        assertFalse(li.hasNext());
    }

    public void testSearchPathEmptyWithSuper() {
        // expect path gets { "/" }
        resourceResolver.setSearchPath((String[]) null);

        // set resource super type
        resourceSuperType = "foo:superBar";
        resourceSuperTypePath = ResourceUtil.resourceTypeToPath(resourceSuperType);
        resource.setResourceSuperType(resourceSuperType);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /foo/bar
        assertTrue(li.hasNext());
        assertEquals("/" + resourceTypePath, li.next());

        // 2. /foo/superBar
        assertTrue(li.hasNext());
        assertEquals("/" + resourceSuperTypePath, li.next());

        // 3. /sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals("/" + DEFAULT_SERVLET_NAME, li.next());

        // 4. finished
        assertFalse(li.hasNext());
    }

    public void testSearchPath1ElementWithSuper() {
        String root0 = "/apps";
        resourceResolver.setSearchPath(root0);

        // set resource super type
        resourceSuperType = "foo:superBar";
        resourceSuperTypePath = ResourceUtil.resourceTypeToPath(resourceSuperType);
        resource.setResourceSuperType(resourceSuperType);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /apps/foo/bar
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + resourceTypePath, li.next());

        // 2. /apps/foo/superBar
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + resourceSuperTypePath, li.next());

        // 3. /apps/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 4. finished
        assertFalse(li.hasNext());
    }

    public void testSearchPath2ElementsWithSuper() {
        String root0 = "/apps";
        String root1 = "/libs";
        resourceResolver.setSearchPath(root0, root1);

        // set resource super type
        resourceSuperType = "foo:superBar";
        resourceSuperTypePath = ResourceUtil.resourceTypeToPath(resourceSuperType);
        resource.setResourceSuperType(resourceSuperType);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /apps/foo/bar
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + resourceTypePath, li.next());

        // 2. /libs/foo/bar
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + resourceTypePath, li.next());

        // 3. /apps/foo/superBar
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + resourceSuperTypePath, li.next());

        // 4. /libs/foo/superBar
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + resourceSuperTypePath, li.next());

        // 5. /apps/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 6. /libs/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 7. finished
        assertFalse(li.hasNext());
    }

    public void testSearchPathEmptyAbsoluteTypeWithSuper() {
        // expect path gets { "/" }
        resourceResolver.setSearchPath((String[]) null);

        // absolute resource type
        resourceType = "/foo/bar";
        resourceTypePath = ResourceUtil.resourceTypeToPath(resourceType);
        resource.setResourceType(resourceType);

        // set resource super type
        resourceSuperType = "foo:superBar";
        resourceSuperTypePath = ResourceUtil.resourceTypeToPath(resourceSuperType);
        resource.setResourceSuperType(resourceSuperType);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /foo/bar
        assertTrue(li.hasNext());
        assertEquals(resourceTypePath, li.next());

        // 2. /foo/superBar
        assertTrue(li.hasNext());
        assertEquals("/" + resourceSuperTypePath, li.next());

        // 3. /sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals("/" + DEFAULT_SERVLET_NAME, li.next());

        // 4. finished
        assertFalse(li.hasNext());
    }

    public void testSearchPath1ElementAbsoluteTypeWithSuper() {
        String root0 = "/apps";
        resourceResolver.setSearchPath(root0);

        // absolute resource type
        resourceType = "/foo/bar";
        resourceTypePath = ResourceUtil.resourceTypeToPath(resourceType);
        resource.setResourceType(resourceType);

        // set resource super type
        resourceSuperType = "foo:superBar";
        resourceSuperTypePath = ResourceUtil.resourceTypeToPath(resourceSuperType);
        resource.setResourceSuperType(resourceSuperType);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /foo/bar
        assertTrue(li.hasNext());
        assertEquals(resourceTypePath, li.next());

        // 2. /apps/foo/superBar
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + resourceSuperTypePath, li.next());

        // 3. /apps/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 4. finished
        assertFalse(li.hasNext());
    }

    public void testSearchPath2ElementsAbsoluteTypeWithSuper() {
        String root0 = "/apps";
        String root1 = "/libs";
        resourceResolver.setSearchPath(root0, root1);

        // absolute resource type
        resourceType = "/foo/bar";
        resourceTypePath = ResourceUtil.resourceTypeToPath(resourceType);
        resource.setResourceType(resourceType);

        // set resource super type
        resourceSuperType = "foo:superBar";
        resourceSuperTypePath = ResourceUtil.resourceTypeToPath(resourceSuperType);
        resource.setResourceSuperType(resourceSuperType);

        final Resource r = request.getResource();
        LocationIterator li = getLocationIterator(r.getResourceType(),
                r.getResourceSuperType());

        // 1. /foo/bar
        assertTrue(li.hasNext());
        assertEquals(resourceTypePath, li.next());

        // 2. /apps/foo/superBar
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + resourceSuperTypePath, li.next());

        // 3. /libs/foo/superBar
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + resourceSuperTypePath, li.next());

        // 4. /apps/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 5. /libs/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 6. finished
        assertFalse(li.hasNext());
    }

    public void testScriptNameWithoutResourceType() {
        String root0 = "/apps";
        String root1 = "/libs";
        resourceResolver.setSearchPath(root0, root1);
        LocationIterator li = getLocationIterator("",
                null,
                "");
        assertTrue(li.hasNext());
        assertEquals("/apps/", li.next());
        assertTrue(li.hasNext());
        assertEquals("/libs/", li.next());
        assertFalse(li.hasNext());
    }

    public void testScriptNameWithResourceType() {
        String root0 = "/apps";
        String root1 = "/libs";
        resourceResolver.setSearchPath(root0, root1);
        LocationIterator li = getLocationIterator("a/b",
                null);
        assertTrue(li.hasNext());
        assertEquals(root0 + "/a/b", li.next());
        assertTrue(li.hasNext());
        assertEquals(root1 + "/a/b", li.next());
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + DEFAULT_SERVLET_NAME, li.next());
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + DEFAULT_SERVLET_NAME, li.next());
        assertFalse(li.hasNext());
    }

    public void testScriptNameWithResourceTypeAndSuperType() {
        String root0 = "/apps";
        String root1 = "/libs";
        resourceResolver.setSearchPath(root0, root1);
        LocationIterator li = getLocationIterator("a/b",
                "c/d");
        assertTrue(li.hasNext());
        assertEquals(root0 + "/a/b", li.next());
        assertTrue(li.hasNext());
        assertEquals(root1 + "/a/b", li.next());
        assertTrue(li.hasNext());
        assertEquals(root0 + "/c/d", li.next());
        assertTrue(li.hasNext());
        assertEquals(root1 + "/c/d", li.next());
        assertTrue(li.hasNext());
        assertEquals(root0 + "/" + DEFAULT_SERVLET_NAME, li.next());
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + DEFAULT_SERVLET_NAME, li.next());
        assertFalse(li.hasNext());
    }

    public void testCircularResourceTypeHierarchy() {
        final String root1 = "/libs";
        resourceResolver.setSearchPath(root1);

        // resource type and super type for start resource
        final String resourceType = "foo/bar";
        final String resourceSuperType = "foo/check1";
        final String resourceSuperType2 = "foo/check2";

        final Resource resource2 = new MockResource(resourceResolver,
                root1 + '/' + resourceSuperType,
                resourceType, resourceSuperType2);
        resourceResolver.addResource(resource2);
        final Resource resource3 = new MockResource(resourceResolver,
                root1 + '/' + resourceSuperType2,
                resourceType, resourceType);
        resourceResolver.addResource(resource3);

        LocationIterator li = getLocationIterator(resourceType,
                resourceSuperType);

        // 1. /libs/foo/bar
        assertTrue(li.hasNext());
        assertEquals(root1 + '/' + resourceType, li.next());

        // 1. /libs/foo/check1
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + resourceSuperType, li.next());

        // 3. /libs/foo/check2
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + resourceSuperType2, li.next());

        // 4. /libs/sling/servlet/default
        assertTrue(li.hasNext());
        assertEquals(root1 + "/" + DEFAULT_SERVLET_NAME, li.next());

        // 5. finished
        assertFalse(li.hasNext());
    }
}
