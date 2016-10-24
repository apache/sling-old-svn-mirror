/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.core.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "deprecation"})
public class ScriptingResourceResolverTest {

    private static final String PATH = "path";
    public static final String QUERY = "query";
    public static final String LANGUAGE = "language";
    private static ScriptingResourceResolver scriptingResourceResolver;
    private static ResourceResolver delegate = mock(ResourceResolver.class);
    private static ResourceResolver delegateClone = mock(ResourceResolver.class);
    private static final String[] searchPaths = {"/apps", "/libs"};

    @BeforeClass
    public static void setUpTestSuite() throws LoginException {
        when(delegate.clone(null)).thenReturn(delegateClone);
        when(delegateClone.getSearchPath()).thenReturn(searchPaths);
        scriptingResourceResolver = new ScriptingResourceResolver(false, delegate);
    }

    @Test
    public void testResolve() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Resource result = mock(Resource.class);
        when(delegate.resolve(request, PATH)).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.resolve(request, PATH));
        verify(delegate).resolve(request, PATH);
    }

    @Test
    public void testResolve1() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Resource result = mock(Resource.class);
        when(delegate.resolve(request)).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.resolve(request));
        verify(delegate).resolve(request);
    }

    @Test
    public void testResolve2() throws Exception {
        final Resource result = mock(Resource.class);
        when(delegate.resolve(PATH)).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.resolve(PATH));
        verify(delegate).resolve(PATH);
    }

    @Test
    public void testMap() throws Exception {
        when(delegate.map(PATH)).thenReturn(PATH);
        assertEquals(PATH, scriptingResourceResolver.map(PATH));
        verify(delegate).map(PATH);
    }

    @Test
    public void testMap1() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(delegate.map(request, PATH)).thenReturn(PATH);
        assertEquals(PATH, scriptingResourceResolver.map(request, PATH));
        verify(delegate).map(request, PATH);
    }

    @Test
    public void testGetResource() throws Exception {
        final Resource result = mock(Resource.class);
        when(delegate.getResource(PATH)).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.getResource(PATH));
        verify(delegate).getResource(PATH);
    }

    @Test
    public void testGetResource1() throws Exception {
        final Resource result = mock(Resource.class);
        final Resource base = mock(Resource.class);
        when(delegate.getResource(base, PATH)).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.getResource(base, PATH));
        verify(delegate).getResource(base, PATH);
    }

    @Test
    public void testGetSearchPath() throws Exception {
        when(delegate.getSearchPath()).thenReturn(searchPaths);
        assertArrayEquals(searchPaths, scriptingResourceResolver.getSearchPath());
        verify(delegate).getSearchPath();
    }

    @Test
    public void testListChildren() throws Exception {
        final Iterator<Resource> resourceIterator = mock(Iterator.class);
        final Resource base = mock(Resource.class);
        when(delegate.listChildren(base)).thenReturn(resourceIterator);
        assertEquals(resourceIterator, scriptingResourceResolver.listChildren(base));
        verify(delegate).listChildren(base);
    }

    @Test
    public void testGetParent() throws Exception {
        final Resource parent = mock(Resource.class);
        final Resource base = mock(Resource.class);
        when(delegate.getParent(base)).thenReturn(parent);
        assertEquals(parent, scriptingResourceResolver.getParent(base));
        verify(delegate).getParent(base);
    }

    @Test
    public void testGetChildren() throws Exception {
        final List<Resource> children = mock(List.class);
        final Resource base = mock(Resource.class);
        when(delegate.getChildren(base)).thenReturn(children);
        assertEquals(children, scriptingResourceResolver.getChildren(base));
        verify(delegate).getChildren(base);
    }

    @Test
    public void testFindResources() throws Exception {
        final Iterator<Resource> result = mock(Iterator.class);
        when(delegate.findResources(QUERY, LANGUAGE)).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.findResources(QUERY, LANGUAGE));
        verify(delegate).findResources(QUERY, LANGUAGE);
    }

    @Test
    public void testQueryResources() throws Exception {
        final Iterator<Map<String, Object>> result = mock(Iterator.class);
        when(delegate.queryResources(QUERY, LANGUAGE)).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.queryResources(QUERY, LANGUAGE));
        verify(delegate).queryResources(QUERY, LANGUAGE);
    }

    @Test
    public void testHasChildren() throws Exception {
        final Resource base = mock(Resource.class);
        when(delegate.hasChildren(base)).thenReturn(true);
        assertTrue(scriptingResourceResolver.hasChildren(base));
        verify(delegate).hasChildren(base);
    }

    @Test
    public void testClone() throws Exception {
        final Map<String, Object> authenticationInfo = mock(Map.class);
        final ResourceResolver result = scriptingResourceResolver.clone(authenticationInfo);
        assertTrue(result instanceof ScriptingResourceResolver);
        final ScriptingResourceResolver resultS = (ScriptingResourceResolver) result;
        assertArrayEquals(searchPaths, resultS.getSearchPath());
        verify(delegateClone).getSearchPath();
    }

    @Test
    public void testIsLive() throws Exception {
        when(delegate.isLive()).thenReturn(true);
        assertTrue(scriptingResourceResolver.isLive());
        verify(delegate).isLive();
    }

    @Test
    public void testClose() throws Exception {
        scriptingResourceResolver.close();
        verify(delegate, times(0)).close();
    }

    @Test
    public void test_close() throws Exception {
        scriptingResourceResolver._close();
        verify(delegate).close();
    }

    @Test
    public void testGetUserID() throws Exception {
        when(delegate.getUserID()).thenReturn("sling-scripting");
        assertEquals("sling-scripting", scriptingResourceResolver.getUserID());
        verify(delegate).getUserID();
    }

    @Test
    public void testGetAttributeNames() throws Exception {
        final Iterator<String> result = mock(Iterator.class);
        when(delegate.getAttributeNames()).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.getAttributeNames());
        verify(delegate).getAttributeNames();
    }

    @Test
    public void testGetAttribute() throws Exception {
        final String attributeName = "attributeName";
        final String attributeValue = "attributeValue";
        when(delegate.getAttribute(attributeName)).thenReturn(attributeValue);
        assertEquals(attributeValue, scriptingResourceResolver.getAttribute(attributeName));
        verify(delegate).getAttribute(attributeName);
    }

    @Test
    public void testDelete() throws Exception {
        final Resource resource = mock(Resource.class);
        scriptingResourceResolver.delete(resource);
        verify(delegate).delete(resource);
    }

    @Test
    public void testCreate() throws Exception {
        final Resource parent = mock(Resource.class);
        final Map<String, Object> properties = mock(Map.class);
        final Resource result = mock(Resource.class);
        when(delegate.create(parent, PATH, properties)).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.create(parent, PATH, properties));
        verify(delegate).create(parent, PATH, properties);
    }

    @Test
    public void testRevert() throws Exception {
        scriptingResourceResolver.revert();
        verify(delegate).revert();
    }

    @Test
    public void testCommit() throws Exception {
        scriptingResourceResolver.commit();
        verify(delegate).commit();
    }

    @Test
    public void testHasChanges() throws Exception {
        when(delegate.hasChanges()).thenReturn(true);
        assertTrue(scriptingResourceResolver.hasChanges());
        verify(delegate).hasChanges();
    }

    @Test
    public void testGetParentResourceType() throws Exception {
        final String resourceType = "a/b/c";
        final Resource resource = mock(Resource.class);
        when(delegate.getParentResourceType(resource)).thenReturn(resourceType);
        assertEquals(resourceType, scriptingResourceResolver.getParentResourceType(resource));
        verify(delegate).getParentResourceType(resource);
    }

    @Test
    public void testGetParentResourceType1() throws Exception {
        final String resourceType = "a/b/c";
        when(delegate.getParentResourceType(PATH)).thenReturn(resourceType);
        assertEquals(resourceType, scriptingResourceResolver.getParentResourceType(PATH));
        verify(delegate).getParentResourceType(PATH);
    }

    @Test
    public void testIsResourceType() throws Exception {
        final Resource resource = mock(Resource.class);
        final String resourceType = "a/b/c";
        when(delegate.isResourceType(resource, resourceType)).thenReturn(true);
        assertTrue(scriptingResourceResolver.isResourceType(resource, resourceType));
        verify(delegate).isResourceType(resource, resourceType);
    }

    @Test
    public void testRefresh() throws Exception {
        scriptingResourceResolver.refresh();
        verify(delegate).refresh();
    }

    @Test
    public void testCopy() throws Exception {
        final String source = "source";
        final String destination = "destination";
        final Resource result = mock(Resource.class);
        when(delegate.copy(source, destination)).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.copy(source, destination));
        verify(delegate).copy(source, destination);
    }

    @Test
    public void testMove() throws Exception {
        final String source = "source";
        final String destination = "destination";
        final Resource result = mock(Resource.class);
        when(delegate.move(source, destination)).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.move(source, destination));
        verify(delegate).move(source, destination);
    }

    @Test
    public void testAdaptTo() throws Exception {
        final String result = "result";
        when(delegate.adaptTo(String.class)).thenReturn(result);
        assertEquals(result, scriptingResourceResolver.adaptTo(String.class));
        verify(delegate).adaptTo(String.class);
    }
}
