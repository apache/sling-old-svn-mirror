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
package org.apache.sling.api.wrappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceResolverWrapperTest {

    private static final String PATH = "path";
    private static final String MAPPED_PATH = "mappedPath";
    private static final String[] searchPaths = {"/apps", "/libs"};
    private static final String QUERY = "query";
    private static final String LANGUAGE = "language";
    private static final String USER = "user";
    private static final String ATTR_NAME = "attrName";
    private ResourceResolver wrappedResolver;
    private ResourceResolverWrapper underTest;

    @Before
    public void setUp() throws Exception {
        wrappedResolver = mock(ResourceResolver.class);
        when(wrappedResolver.getSearchPath()).thenReturn(searchPaths);
        underTest = new ResourceResolverWrapper(wrappedResolver);
    }

    @Test
    public void testResolve() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn(PATH);
        when(wrappedResolver.resolve(request, PATH)).thenReturn(resource);
        final Resource result = underTest.resolve(request, PATH);

        assertTrue(result instanceof ResourceWrapper);
        assertEquals(underTest, result.getResourceResolver());
        assertEquals(resource.getPath(), result.getPath());
        verify(wrappedResolver).resolve(request, PATH);
    }

    @Test
    public void testResolve1() throws Exception {
        final Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn(PATH);
        when(wrappedResolver.resolve(PATH)).thenReturn(resource);
        final Resource result = underTest.resolve(PATH);

        assertTrue(result instanceof ResourceWrapper);
        assertEquals(underTest, result.getResourceResolver());
        assertEquals(resource.getPath(), result.getPath());
        verify(wrappedResolver).resolve(PATH);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testResolve2() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn(PATH);
        when(wrappedResolver.resolve(request)).thenReturn(resource);
        final Resource result = underTest.resolve(request);

        assertTrue(result instanceof ResourceWrapper);
        assertEquals(underTest, result.getResourceResolver());
        assertEquals(resource.getPath(), result.getPath());
        verify(wrappedResolver).resolve(request);
    }

    @Test
    public void testMap() throws Exception {
        when(wrappedResolver.map(PATH)).thenReturn(MAPPED_PATH);

        assertEquals(MAPPED_PATH, underTest.map(PATH));
        verify(wrappedResolver).map(PATH);
    }

    @Test
    public void testMap1() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(wrappedResolver.map(request, PATH)).thenReturn(MAPPED_PATH);

        assertEquals(MAPPED_PATH, underTest.map(request, PATH));
        verify(wrappedResolver).map(request, PATH);
    }

    @Test
    public void testGetResource() throws Exception {
        final Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn(PATH);
        when(wrappedResolver.getResource(PATH)).thenReturn(resource);

        final Resource result = underTest.getResource(PATH);
        assertTrue(result instanceof ResourceWrapper);
        assertEquals(underTest, result.getResourceResolver());
        assertEquals(resource.getPath(), result.getPath());
        verify(wrappedResolver).getResource(PATH);
    }

    @Test
    public void testGetResource1() throws Exception {
        final Resource parent = mock(Resource.class);
        final Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn(PATH);
        when(wrappedResolver.getResource(parent, PATH)).thenReturn(resource);

        final Resource result = underTest.getResource(parent, PATH);
        assertTrue(result instanceof ResourceWrapper);
        assertEquals(underTest, result.getResourceResolver());
        assertEquals(resource.getPath(), result.getPath());
        verify(wrappedResolver).getResource(parent, PATH);
    }

    @Test
    public void testGetSearchPath() throws Exception {
        assertArrayEquals(searchPaths, underTest.getSearchPath());
        verify(wrappedResolver).getSearchPath();
    }

    @Test
    public void testListChildren() throws Exception {
        final Resource parent = mock(Resource.class);
        final List<Resource> children = new ArrayList<>(1);
        final Resource child = mock(Resource.class);
        when(child.getPath()).thenReturn(PATH);
        children.add(child);
        when(wrappedResolver.listChildren(parent)).thenAnswer(new Answer<Iterator<Resource>>() {
            @Override
            public Iterator<Resource> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return children.iterator();
            }
        });

        int index = 0;
        Iterator<Resource> wrappedIterator = underTest.listChildren(parent);
        assertTrue(wrappedIterator instanceof IteratorWrapper);
        while (wrappedIterator.hasNext()) {
            Resource result = wrappedIterator.next();
            assertTrue(result instanceof ResourceWrapper);
            assertEquals(PATH, result.getPath());
            index++;
        }
        assertEquals(1, index);
        verify(wrappedResolver).listChildren(parent);
    }

    @Test
    public void testGetParent() throws Exception {
        final Resource parent = mock(Resource.class);
        final Resource child = mock(Resource.class);
        when(parent.getPath()).thenReturn(PATH);
        when(wrappedResolver.getParent(child)).thenReturn(parent);

        Resource result = underTest.getParent(child);
        assertTrue(result instanceof ResourceWrapper);
        assertEquals(parent.getPath(), result.getPath());
        verify(wrappedResolver).getParent(child);
    }

    @Test
    public void testGetChildren() throws Exception {
        final Resource parent = mock(Resource.class);
        final List<Resource> children = new ArrayList<>(1);
        final Resource child = mock(Resource.class);
        when(child.getPath()).thenReturn(PATH);
        children.add(child);
        when(wrappedResolver.getChildren(parent)).thenReturn(children);
        int index = 0;
        Iterable<Resource> iterable = underTest.getChildren(parent);
        for (Resource result : iterable) {
            assertTrue(result instanceof ResourceWrapper);
            assertEquals(PATH, result.getPath());
            index++;
        }
        assertEquals(1, index);
        verify(wrappedResolver).getChildren(parent);
    }

    @Test
    public void testFindResources() throws Exception {
        final List<Resource> children = new ArrayList<>(1);
        final Resource child = mock(Resource.class);
        when(child.getPath()).thenReturn(PATH);
        children.add(child);
        when(wrappedResolver.findResources(QUERY, LANGUAGE)).thenAnswer(new Answer<Iterator<Resource>>() {
            @Override
            public Iterator<Resource> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return children.iterator();
            }
        });

        int index = 0;
        final Iterator<Resource> wrappedIterator = underTest.findResources(QUERY, LANGUAGE);
        assertTrue(wrappedIterator instanceof IteratorWrapper);
        while (wrappedIterator.hasNext()) {
            Resource result = wrappedIterator.next();
            assertTrue(result instanceof ResourceWrapper);
            assertEquals(PATH, result.getPath());
            index++;
        }
        assertEquals(1, index);
        verify(wrappedResolver).findResources(QUERY, LANGUAGE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testQueryResources() throws Exception {
        final Map<String, Object> expected = mock(Map.class);
        final List<Map<String, Object>> list = new ArrayList<>();
        list.add(expected);
        when(wrappedResolver.queryResources(QUERY, LANGUAGE)).thenReturn(list.iterator());

        int index = 0;
        final Iterator<Map<String, Object>> iterator = underTest.queryResources(QUERY, LANGUAGE);
        Map<String, Object> result = null;
        while (iterator.hasNext()) {
            result = iterator.next();
            index++;
        }
        assertEquals(expected, result);
        assertEquals(1, index);
        verify(wrappedResolver).queryResources(QUERY, LANGUAGE);
    }

    @Test
    public void testHasChildren() throws Exception {
        final Resource resource = mock(Resource.class);
        when(wrappedResolver.hasChildren(resource)).thenReturn(true);

        assertTrue(underTest.hasChildren(resource));
        verify(wrappedResolver).hasChildren(resource);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testClone() throws Exception {
        final Map<String, Object> authenticationInfo = mock(Map.class);
        final ResourceResolver clone = mock(ResourceResolver.class);
        when(wrappedResolver.clone(authenticationInfo)).thenReturn(clone);

        final ResourceResolver result = underTest.clone(authenticationInfo);
        assertTrue(result instanceof ResourceResolverWrapper);
        assertNotEquals(result, underTest);
        verify(wrappedResolver).clone(authenticationInfo);
    }

    @Test
    public void testIsLive() throws Exception {
        when(wrappedResolver.isLive()).thenReturn(true);
        assertTrue(underTest.isLive());
        verify(wrappedResolver).isLive();
    }

    @Test
    public void testClose() throws Exception {
        underTest.close();
        verify(wrappedResolver).close();
    }

    @Test
    public void testGetUserID() throws Exception {
        when(wrappedResolver.getUserID()).thenReturn(USER);
        assertEquals(USER, underTest.getUserID());
        verify(wrappedResolver).getUserID();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAttributeNames() throws Exception {
        final Iterator<String> attributeNames = mock(Iterator.class);
        when(wrappedResolver.getAttributeNames()).thenReturn(attributeNames);

        assertEquals(attributeNames, underTest.getAttributeNames());
        verify(wrappedResolver).getAttributeNames();
    }

    @Test
    public void testGetAttribute() throws Exception {
        final Object obj = mock(Object.class);
        when(wrappedResolver.getAttribute(ATTR_NAME)).thenReturn(obj);

        assertEquals(obj, underTest.getAttribute(ATTR_NAME));
        verify(wrappedResolver).getAttribute(ATTR_NAME);
    }

    @Test
    public void testDelete() throws Exception {
        final Resource toDelete = mock(Resource.class);
        underTest.delete(toDelete);
        verify(wrappedResolver).delete(toDelete);
    }

    @Test
    public void testCreate() throws Exception {
        final Resource parent = mock(Resource.class);
        final String name = "aName";
        @SuppressWarnings("serial")
        final Map<String, Object> properties = new HashMap<String, Object>(){{
            put("jcr:primaryType", "nt:unstructured");
        }};
        final Resource expected = mock(Resource.class);
        when(expected.getParent()).thenReturn(parent);
        when(expected.getName()).thenReturn(name);
        when(wrappedResolver.create(parent, name, properties)).thenReturn(expected);

        final Resource result = underTest.create(parent, name, properties);
        assertTrue(result instanceof ResourceWrapper);
        assertEquals(parent, result.getParent());
        assertEquals(name, result.getName());
        verify(wrappedResolver).create(parent, name, properties);
    }

    @Test
    public void testRevert() throws Exception {
        underTest.revert();
        verify(wrappedResolver).revert();
    }

    @Test
    public void testCommit() throws Exception {
        underTest.commit();
        verify(wrappedResolver).commit();
    }

    @Test
    public void testHasChanges() throws Exception {
        when(wrappedResolver.hasChanges()).thenReturn(true);
        assertTrue(underTest.hasChanges());
        verify(wrappedResolver).hasChanges();
    }

    @Test
    public void testGetParentResourceType() throws Exception {
        final String rt = "rt";
        final Resource resource = mock(Resource.class);
        when(wrappedResolver.getParentResourceType(resource)).thenReturn(rt);

        assertEquals(rt, underTest.getParentResourceType(resource));
        verify(wrappedResolver).getParentResourceType(resource);
    }

    @Test
    public void testGetParentResourceType1() throws Exception {
        final String rt = "rt";
        final String rtt = "rtt";
        when(wrappedResolver.getParentResourceType(rt)).thenReturn(rtt);

        assertEquals(rtt, underTest.getParentResourceType(rt));
        verify(wrappedResolver).getParentResourceType(rt);
    }

    @Test
    public void testIsResourceType() throws Exception {
        final Resource resource = mock(Resource.class);
        final String rt = "rt";
        when(wrappedResolver.isResourceType(resource, rt)).thenReturn(true);

        assertTrue(underTest.isResourceType(resource, rt));
        verify(wrappedResolver).isResourceType(resource, rt);
    }

    @Test
    public void testRefresh() throws Exception {
        underTest.refresh();
        verify(wrappedResolver).refresh();
    }

    @Test
    public void testCopy() throws Exception {
        final String source = "source";
        final String destination = "destination";
        final Resource expected = mock(Resource.class);
        when(expected.getPath()).thenReturn(destination);
        when(wrappedResolver.copy(source, destination)).thenReturn(expected);

        final Resource result = underTest.copy(source, destination);
        assertTrue(result instanceof ResourceWrapper);
        assertEquals(underTest, result.getResourceResolver());
        assertEquals(destination, result.getPath());
        verify(wrappedResolver).copy(source, destination);
    }

    @Test
    public void testMove() throws Exception {
        final String source = "source";
        final String destination = "destination";
        final Resource expected = mock(Resource.class);
        when(expected.getPath()).thenReturn(destination);
        when(wrappedResolver.move(source, destination)).thenReturn(expected);

        final Resource result = underTest.move(source, destination);
        assertTrue(result instanceof ResourceWrapper);
        assertEquals(underTest, result.getResourceResolver());
        assertEquals(destination, result.getPath());
        verify(wrappedResolver).move(source, destination);
    }

    @Test
    public void testAdaptTo() throws Exception {
        List list = mock(List.class);
        when(wrappedResolver.adaptTo(List.class)).thenReturn(list);

        assertEquals(list, underTest.adaptTo(List.class));
        verify(wrappedResolver).adaptTo(List.class);
    }
}
