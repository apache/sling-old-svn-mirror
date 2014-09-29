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
package org.apache.sling.superimposing.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

@RunWith(MockitoJUnitRunner.class)
public class SuperimposingResourceProviderImplTest {

    private SuperimposingResourceProviderImpl underTest;
    private SuperimposingResourceProviderImpl underTestOverlay;

    @Mock
    private BundleContext bundleContext;
    @Mock
    private ServiceRegistration serviceRegistration;
    @Mock
    private ServiceRegistration serviceRegistrationOverlay;
    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private Session session;
    @Mock
    private Resource originalRootResource;
    @Mock
    private Resource originalSubResource;

    private static final String ORIGINAL_PATH = "/root/path1";
    private static final String SUPERIMPOSED_PATH = "/root/path2";
    private static final String RESOURCE_TYPE = "/resourceType1";

    @Before
    public void setUp() {
        // setup a superimposing resource provider without overlay
        underTest = new SuperimposingResourceProviderImpl(SUPERIMPOSED_PATH, ORIGINAL_PATH, false);
        when(bundleContext.registerService(anyString(), eq(underTest), any(Dictionary.class))).thenReturn(serviceRegistration);
        underTest.registerService(bundleContext);

        // and one with overlay
        underTestOverlay = new SuperimposingResourceProviderImpl(SUPERIMPOSED_PATH, ORIGINAL_PATH, true);
        when(bundleContext.registerService(anyString(), eq(underTestOverlay), any(Dictionary.class))).thenReturn(serviceRegistrationOverlay);
        underTestOverlay.registerService(bundleContext);

        // prepare test resources
        prepareOriginalResource(originalRootResource, ORIGINAL_PATH);
        prepareOriginalResource(originalSubResource, ORIGINAL_PATH + "/sub1");
        when(resourceResolver.listChildren(originalRootResource)).thenAnswer(new Answer<Iterator<Resource>>() {
            public Iterator<Resource> answer(InvocationOnMock invocation) {
                return Arrays.asList(new Resource[] { originalSubResource }).iterator();
            }
        });
    }

    private void prepareOriginalResource(Resource mockResource, String path) {
        // prepare resource
        when(mockResource.getPath()).thenReturn(path);
        when(mockResource.getResourceType()).thenReturn(RESOURCE_TYPE);
        when(mockResource.getResourceSuperType()).thenReturn(null);
        ResourceMetadata resourceMetadata = new ResourceMetadata();
        resourceMetadata.setResolutionPath(path);
        when(mockResource.getResourceMetadata()).thenReturn(resourceMetadata);
        when(mockResource.getResourceResolver()).thenReturn(resourceResolver);

        // mount in resource tree
        when(resourceResolver.getResource(path)).thenReturn(mockResource);
    }

    @After
    public void tearDown() {
        underTest.unregisterService();
        verify(serviceRegistration).unregister();

        underTestOverlay.unregisterService();
        verify(serviceRegistrationOverlay).unregister();
    }

    @Test
    public void testGetter() {
        assertEquals(SUPERIMPOSED_PATH, underTest.getRootPath());
        assertEquals(ORIGINAL_PATH, underTest.getSourcePath());
        assertFalse(underTest.isOverlayable());
    }

    @Test
    public void testGetterOverlay() {
        assertEquals(SUPERIMPOSED_PATH, underTestOverlay.getRootPath());
        assertEquals(ORIGINAL_PATH, underTestOverlay.getSourcePath());
        assertTrue(underTestOverlay.isOverlayable());
    }

    @Test
    public void testEquals() {
        assertTrue(underTest.equals(underTest));
        assertFalse(underTest.equals(underTestOverlay));
        assertTrue(underTestOverlay.equals(underTestOverlay));
        assertFalse(underTest.equals(underTestOverlay));
    }

    @Test
    public void testGetMappedRootResource() {
        Resource resource = underTest.getResource(resourceResolver, SUPERIMPOSED_PATH);
        assertTrue(resource instanceof SuperimposingResource);
        assertEquals(SUPERIMPOSED_PATH, resource.getPath());

        resource = underTestOverlay.getResource(resourceResolver, SUPERIMPOSED_PATH);
        assertTrue(resource instanceof SuperimposingResource);
        assertEquals(SUPERIMPOSED_PATH, resource.getPath());
    }

    @Test
    public void testGetMappedRootResourceWithOverlay() throws RepositoryException {
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(session.itemExists(SUPERIMPOSED_PATH)).thenReturn(true);

        Resource resource = underTest.getResource(resourceResolver, SUPERIMPOSED_PATH);
        assertTrue(resource instanceof SuperimposingResource);
        assertEquals(SUPERIMPOSED_PATH, resource.getPath());

        // root path cannot be overlayed
        resource = underTestOverlay.getResource(resourceResolver, SUPERIMPOSED_PATH);
        assertTrue(resource instanceof SuperimposingResource);
        assertEquals(SUPERIMPOSED_PATH, resource.getPath());
    }

    @Test
    public void testGetMappedSubResource() {
        Resource resource = underTest.getResource(resourceResolver, SUPERIMPOSED_PATH + "/sub1");
        assertTrue(resource instanceof SuperimposingResource);
        assertEquals(SUPERIMPOSED_PATH + "/sub1", resource.getPath());

        resource = underTestOverlay.getResource(resourceResolver, SUPERIMPOSED_PATH + "/sub1");
        assertTrue(resource instanceof SuperimposingResource);
        assertEquals(SUPERIMPOSED_PATH + "/sub1", resource.getPath());
    }

    @Test
    public void testGetMappedSubResourceWithOverlay() throws RepositoryException {
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(session.itemExists(SUPERIMPOSED_PATH + "/sub1")).thenReturn(true);

        Resource resource = underTest.getResource(resourceResolver, SUPERIMPOSED_PATH + "/sub1");
        assertTrue(resource instanceof SuperimposingResource);
        assertEquals(SUPERIMPOSED_PATH + "/sub1", resource.getPath());

        // overlay item exists, allow underlying resource provider to step in
        resource = underTestOverlay.getResource(resourceResolver, SUPERIMPOSED_PATH + "/sub1");
        assertNull(resource);
    }

    @Test
    public void testGetMappedNonExistingResource() {
        Resource resource = underTest.getResource(resourceResolver, SUPERIMPOSED_PATH + "/sub2");
        assertNull(resource);

        resource = underTestOverlay.getResource(resourceResolver, SUPERIMPOSED_PATH + "/sub2");
        assertNull(resource);
    }

    @Test
    public void testGetMappedNonExistingResourceWithOverlay() throws RepositoryException {
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(session.itemExists(SUPERIMPOSED_PATH + "/sub2")).thenReturn(true);

        Resource resource = underTest.getResource(resourceResolver, SUPERIMPOSED_PATH + "/sub2");
        assertNull(resource);

        resource = underTestOverlay.getResource(resourceResolver, SUPERIMPOSED_PATH + "/sub2");
        assertNull(resource);
    }

    @Test
    public void testGetMappedResourceRootInvalidPath() {
        Resource resource = underTest.getResource(resourceResolver, "/invalid/path");
        assertNull(resource);

        resource = underTestOverlay.getResource(resourceResolver, "/invalid/path");
        assertNull(resource);
    }

    @Test
    public void testListChildren() {
        Resource resource = underTest.getResource(resourceResolver, SUPERIMPOSED_PATH);
        Iterator<Resource> iterator = underTest.listChildren(resource);
        assertTrue(iterator.hasNext());
        assertEquals(SUPERIMPOSED_PATH + "/sub1", iterator.next().getPath());
    }

    @Test
    public void testListChildrenWithResourceWrapper() {
        Resource resource = underTest.getResource(resourceResolver, SUPERIMPOSED_PATH);
        Iterator<Resource> iterator = underTest.listChildren(new ResourceWrapper(resource));
        assertTrue(iterator.hasNext());
        assertEquals(SUPERIMPOSED_PATH + "/sub1", iterator.next().getPath());
    }

    @Test
    public void testListChildrenNonSuperimposingResource() {
        Iterator<Resource> iterator = underTest.listChildren(mock(Resource.class));
        assertNull(iterator);
    }

}
