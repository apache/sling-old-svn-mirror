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
package org.apache.sling.resource.collection.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.resource.collection.ResourceCollection;
import org.apache.sling.resource.collection.ResourceCollectionManager;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;

public class ResourceCollectionImplTest {
	private ResourceResolver resResolver;
    private ResourceCollectionManager rcm;

	@Before
	public void setUp() throws Exception {
		resResolver = new MockResourceResolverFactory().getAdministrativeResourceResolver(null);
		rcm = new ResourceCollectionManagerImpl();
	}

	@Test
	public void testAddResource() throws Exception {

        final ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "test1");
        final Resource res1 = resResolver.create(resResolver.getResource("/"), "res1",
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));
        collection.add(res1);
        final Resource resource = resResolver.create(resResolver.getResource("/"), "res2",
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));
        collection.add(resource);

        Assert.assertEquals(true, collection.contains(resource));
        Assert.assertEquals(true, collection.contains(resource));
        Assert.assertNotNull(resResolver.getResource("/test1"));
        Assert.assertEquals(ResourceCollection.RESOURCE_TYPE, resResolver.getResource("/test1").getResourceType());
	}

	@Test
	public void testCreateCollection() throws Exception {
        final ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "test1");
        final Resource res1 = resResolver.create(resResolver.getResource("/"), "res1",
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));
        collection.add(res1, null);
        final Resource resource = resResolver.create(resResolver.getResource("/"), "res2",
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));
        collection.add(resource, null);

        Assert.assertEquals(true, collection.contains(resource));
        Assert.assertNotNull(resResolver.getResource("/test1"));
        Assert.assertEquals(ResourceCollection.RESOURCE_TYPE, resResolver.getResource("/test1").getResourceType());
	}

	@Test
	public void testCheckPath() throws Exception {
		final Resource rootResource = resResolver.create(resResolver.getResource("/"), "root",
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));

        final ResourceCollection collection = rcm.createCollection(rootResource, "test1");


        Assert.assertEquals(rootResource.getPath() + "/" + "test1", collection.getPath());
 	}

	@Test
	public void testGetCollection() throws Exception {
        ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "test1");
        final Resource res1 = resResolver.create(resResolver.getResource("/"), "res1",
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));
        collection.add(res1, null);
        final Resource resource = resResolver.create(resResolver.getResource("/"), "res2",
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));
        collection.add(resource, null);

        collection = rcm.getCollection(resResolver.getResource(collection.getPath()));

        Assert.assertEquals(true, collection.contains(resource));
        Assert.assertNotNull(resResolver.getResource("/test1"));
        Assert.assertEquals(ResourceCollection.RESOURCE_TYPE, resResolver.getResource("/test1").getResourceType());
	}

	@Test
	public void testListCollection() throws Exception {
        final ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "collection1");
        final Resource res1 = resResolver.create(resResolver.getResource("/"), "res1",
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));
        collection.add(res1, null);
        final Resource resource = resResolver.create(resResolver.getResource("/"), "res2",
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));

        collection.add(resource, null);
        Assert.assertEquals(true, collection.contains(resource));

        final Iterator<Resource> resources = collection.getResources();
        int numOfRes = 0;
        while (resources.hasNext()) {
        	resources.next();
        	numOfRes ++;
        }

        Assert.assertEquals(2, numOfRes);
	}

	@Test
	public void testCreateCollectionWithProperties() throws Exception {
		final Map<String, Object> props = new HashMap<String, Object>();
		props.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "some/type");
		props.put("creator", "slingdev");

        final ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "collection3", props);
        final Resource resource = resResolver.create(resResolver.getResource("/"), "res1",
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));
        collection.add(resource, null);

        final Resource collectionRes = resResolver.getResource("/collection3");
        Assert.assertNotNull(collectionRes);

        Assert.assertEquals(true, collection.contains(resource));
        Assert.assertEquals(ResourceCollection.RESOURCE_TYPE, collectionRes.getResourceSuperType());

        ValueMap vm = collectionRes.adaptTo(ValueMap.class);

        Assert.assertEquals("slingdev", vm.get("creator", ""));
	}

	@Test
	public void testAddResourceWithProperties() throws Exception {
		final Map<String, Object> props = new HashMap<String, Object>();
		props.put("creator", "slingdev");

        final ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "collection3");

        final Resource resource = resResolver.create(resResolver.getResource("/"), "res1",
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));
        collection.add(resource, props);

        final Resource collectionRes = resResolver.getResource("/collection3");
        Assert.assertNotNull(collectionRes);

        Assert.assertEquals(true, collection.contains(resource));

        ValueMap vm = collection.getProperties(resource);

        if (vm != null) {
        	Assert.assertEquals("slingdev", vm.get("creator", ""));
        } else {
        	Assert.fail("no resource entry in collection");
        }
	}

	@Test
	public void testOrdering() throws Exception {
        final ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "test1");
        String[] resPaths = {"/res1", "/res2"};
        final Resource resource = resResolver.create(resResolver.getResource("/"), resPaths[0].substring(1),
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));

        collection.add(resource, null);
        final Resource resource2 = resResolver.create(resResolver.getResource("/"), resPaths[1].substring(1),
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"type"));
        collection.add(resource2, null);

        Assert.assertEquals(true, collection.contains(resource2));
        Assert.assertNotNull(resResolver.getResource("/test1"));
        Assert.assertEquals(ResourceCollection.RESOURCE_TYPE, resResolver.getResource("/test1").getResourceType());

        Iterator<Resource> resources = collection.getResources();

        int numOfRes = 0;
        while (resources.hasNext()) {
        	Resource entry = resources.next();
        	Assert.assertEquals(resPaths[numOfRes], entry.getPath());
        	numOfRes ++;
        }

        try {
        	collection.orderBefore(resource, resource);
        	Assert.fail("should have thrown IllegalArgument");
        } catch (IllegalArgumentException e) {

        }

        //change the order
        collection.orderBefore(resource2, resource);

        resources = collection.getResources();

        numOfRes = 2;
        while (resources.hasNext()) {
        	numOfRes --;
        	Resource entry = resources.next();
        	Assert.assertEquals(resPaths[numOfRes], entry.getPath());
        }

        Assert.assertEquals(0, numOfRes);
	}
}