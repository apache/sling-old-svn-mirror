package org.apache.sling.resource.collection.impl;


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
import org.apache.sling.resource.collection.test.MockResource;
import org.apache.sling.resource.collection.test.MockResourceResolver;
import org.junit.Before;
import org.junit.Test;

public class ResourceCollectionImplTest {
	private ResourceResolver resResolver;

	@Before
	public void setUp() throws Exception {
		resResolver = new MockResourceResolver();
		//create some root resource
		Resource resource = new MockResource(resResolver, "/", "type");
		
		((MockResourceResolver) resResolver).addResource(resource);
	}
	
	@Test
	public void testAddResource() throws Exception {
		ResourceCollectionManager rcm = new ResourceCollectionManagerImpl(resResolver);
		        
        ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "test1");
        
        Resource resource = new MockResource(resResolver, "/res1", "type");
        ((MockResourceResolver) resResolver).addResource(resource);
        
        collection.add(resource);
        resource = new MockResource(resResolver, "/res2", "type");
        ((MockResourceResolver) resResolver).addResource(resource);
        
        collection.add(resource);
        
        Assert.assertEquals(true, collection.contains(resource));
        
        Assert.assertEquals(true, collection.contains(resource));
        Assert.assertNotNull(resResolver.getResource("/test1"));
        Assert.assertEquals(ResourceCollection.RESOURCE_TYPE, resResolver.getResource("/test1").getResourceType());
	}
	
	@Test
	public void testCreateCollection() throws Exception {
		ResourceCollectionManager rcm = new ResourceCollectionManagerImpl(resResolver);
		        
        ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "test1");
        
        Resource resource = new MockResource(resResolver, "/res1", "type");
        ((MockResourceResolver) resResolver).addResource(resource);
        
        collection.add(resource, null);
        resource = new MockResource(resResolver, "/res2", "type");
        ((MockResourceResolver) resResolver).addResource(resource);
        
        collection.add(resource, null);
        
        Assert.assertEquals(true, collection.contains(resource));
        Assert.assertNotNull(resResolver.getResource("/test1"));
        Assert.assertEquals(ResourceCollection.RESOURCE_TYPE, resResolver.getResource("/test1").getResourceType());
	}
	
	@Test
	public void testGetCollection() throws Exception {
		ResourceCollectionManager rcm = new ResourceCollectionManagerImpl(resResolver);
		        
        ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "test1");
        
        Resource resource = new MockResource(resResolver, "/res1", "type");
        ((MockResourceResolver) resResolver).addResource(resource);
        
        collection.add(resource, null);
        resource = new MockResource(resResolver, "/res2", "type");
        ((MockResourceResolver) resResolver).addResource(resource);
        
        collection.add(resource, null);
        
        collection = rcm.getCollection(resResolver.getResource(collection.getPath()));
        
        Assert.assertEquals(true, collection.contains(resource));
        Assert.assertNotNull(resResolver.getResource("/test1"));
        Assert.assertEquals(ResourceCollection.RESOURCE_TYPE, resResolver.getResource("/test1").getResourceType());
	}
	
	@Test
	public void testListCollection() throws Exception {
		ResourceCollectionManager rcm = new ResourceCollectionManagerImpl(resResolver);
        
        ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "collection1");
        
        Resource resource = new MockResource(resResolver, "/res1", "type");
        ((MockResourceResolver) resResolver).addResource(resource);
        
        collection.add(resource, null);
        resource = new MockResource(resResolver, "/res2", "type");
        ((MockResourceResolver) resResolver).addResource(resource);
        
        collection.add(resource, null);
        
        Assert.assertEquals(true, collection.contains(resource));
        
        Iterator<Resource> resources = collection.getResources();
        
        int numOfRes = 0;
        while (resources.hasNext()) {
        	resources.next();
        	numOfRes ++;
        }
        
        Assert.assertEquals(2, numOfRes);
	}
	
	@Test
	public void testCreateCollectionWithProperties() throws Exception {
		ResourceCollectionManager rcm = new ResourceCollectionManagerImpl(resResolver);
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "some/type");
		props.put("creator", "slingdev");
        
        ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "collection3", props);
        
        Resource resource = new MockResource(resResolver, "/res1", "type");
        ((MockResourceResolver) resResolver).addResource(resource);
        
        collection.add(resource, null);
        
        
        Resource collectionRes = resResolver.getResource("/collection3");
        Assert.assertNotNull(collectionRes);
        
        Assert.assertEquals(true, collection.contains(resource));
        Assert.assertEquals(ResourceCollection.RESOURCE_TYPE, collectionRes.getResourceSuperType());
        
        ValueMap vm = collectionRes.adaptTo(ValueMap.class);
        
        Assert.assertEquals("slingdev", vm.get("creator", ""));
	}
	
	@Test
	public void testAddResourceWithProperties() throws Exception {
		ResourceCollectionManager rcm = new ResourceCollectionManagerImpl(resResolver);
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("creator", "slingdev");
        
        ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "collection3");
        
        Resource resource = new MockResource(resResolver, "/res1", "type");
        ((MockResourceResolver) resResolver).addResource(resource);
        
        collection.add(resource, props);
        
        
        Resource collectionRes = resResolver.getResource("/collection3");
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
		ResourceCollectionManager rcm = new ResourceCollectionManagerImpl(resResolver);
        
        ResourceCollection collection = rcm.createCollection(resResolver.getResource("/"), "test1");
        String[] resPaths = {"/res1", "/res2"};
        Resource resource = new MockResource(resResolver, resPaths[0], "type");
        ((MockResourceResolver) resResolver).addResource(resource);
        
        collection.add(resource, null);
        Resource resource2 = new MockResource(resResolver, resPaths[1], "type");
        ((MockResourceResolver) resResolver).addResource(resource2);
        
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
