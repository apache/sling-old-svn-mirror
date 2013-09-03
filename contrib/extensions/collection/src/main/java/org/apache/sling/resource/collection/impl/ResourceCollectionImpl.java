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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.resource.collection.ResourceCollection;

/**
 * Implements <code>ResourceCollection</code>
 *
 */
public class ResourceCollectionImpl implements
        ResourceCollection {

    private static final Logger log = LoggerFactory.getLogger(ResourceCollectionImpl.class);

    /**
     * Defines the resource type property
     */
    private static final String RESOURCE_TYPE = JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

    /**
     * underlying resource
     */
    private final Resource resource;

    /**
     * The resource resolver in use by the resource.
     */
    private final ResourceResolver resolver;

    private final Resource membersResource;

    /**
     * Creates a new collection from the given resource
     * 
     * @param resource the resource
     */
    public ResourceCollectionImpl(Resource resource) {
        this.resource = resource;
        resolver = resource.getResourceResolver();
        membersResource = resource.getChild(ResourceCollectionConstants.MEMBERS_NODE_NAME);
    }
    
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return resource.getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getPath() {
        return resource.getPath();
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(Resource res, Map<String, Object> properties) throws PersistenceException {
        if (res != null && !contains(res)) {
        	ModifiableValueMap vm = membersResource.adaptTo(ModifiableValueMap.class);
        	String[] order = vm.get(ResourceCollectionConstants.REFERENCES_PROP, new String[]{});
        	
        	order = (String[]) ArrayUtils.add(order, res.getPath());
        	vm.put(ResourceCollectionConstants.REFERENCES_PROP, order);
        	
        	if (properties == null) {
        		properties = new HashMap<String, Object>();
        	}
            properties.put(ResourceCollectionConstants.REF_PROPERTY, res.getPath());
            resolver.create(
                membersResource,
                ResourceUtil.createUniqueChildName(membersResource,
                    res.getName()), properties);
            log.debug("added member to resource {} to collection {}",
                new String[] { res.getPath(), resource.getPath() });
            return true;
        }

        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean add(Resource res) throws PersistenceException {
        if (res != null && !contains(res)) {
        	ModifiableValueMap vm = membersResource.adaptTo(ModifiableValueMap.class);
        	String[] order = vm.get(ResourceCollectionConstants.REFERENCES_PROP, new String[]{});
        	
        	order = (String[]) ArrayUtils.add(order, res.getPath());
        	vm.put(ResourceCollectionConstants.REFERENCES_PROP, order);
        	
        	Map<String, Object> properties = new HashMap<String, Object>();
        	properties.put(ResourceCollectionConstants.REF_PROPERTY, res.getPath());
            resolver.create(
                membersResource,
                ResourceUtil.createUniqueChildName(membersResource,
                    res.getName()), properties);
            log.debug("added member to resource {} to collection {}",
                new String[] { res.getPath(), resource.getPath() });
            return true;
        }

        return false;
    }

    

    /**
     * {@inheritDoc}
     */
    public Iterator<Resource> getResources() {
    	
    	ValueMap vm = membersResource.adaptTo(ValueMap.class);
    	String[] references = vm.get(ResourceCollectionConstants.REFERENCES_PROP, new String[]{});
    	List<Resource> resources = new ArrayList<Resource>();
    	
        for (String path:references) {
        	Resource resource = resolver.getResource(path);
        	if (resource != null){
        		resources.add(resource);
        	}
        }
        
		return resources.iterator();
    }

    /**
     * Returns the sling resource type on content node of collection
     * 
     * @param
     * @return <code>sling:resourceType</code> for the collection resource
     */
    public String getType() {
        return resource.getResourceType();
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Resource res) {
    	if (res != null) {
    		ValueMap vm = membersResource.adaptTo(ValueMap.class);
        	String[] order = vm.get(ResourceCollectionConstants.REFERENCES_PROP, new String[]{});
        	
        	int index = ArrayUtils.indexOf(order, res.getPath(), 0);
        	
        	return index >= 0 ? true: false;
    	}
    	
    	return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Resource res) throws PersistenceException {
    	//remove the resource
        Resource tobeRemovedRes = findRes(res);
        if (tobeRemovedRes == null) {
        	return false;
        }
        resolver.delete(tobeRemovedRes);
        //remove from order array
        ModifiableValueMap vm = membersResource.adaptTo(ModifiableValueMap.class);
    	String[] order = vm.get(ResourceCollectionConstants.REFERENCES_PROP, new String[]{});
    	
    	int index = ArrayUtils.indexOf(order, res.getPath(), 0);
    	
    	order = (String[]) ArrayUtils.remove(order, index);
    	vm.put(ResourceCollectionConstants.REFERENCES_PROP, order);
    	
    	return true;
    }

    /**
     * Sets the sling resource type on content node of collection
     * 
     * @param type <code>sling:resourceType</code> to be set on the content node
     * @return
     */
    public void setType(String type) throws PersistenceException {
        ModifiableValueMap mvp = resource.adaptTo(ModifiableValueMap.class);
        mvp.put(RESOURCE_TYPE, type);
    }

    private Resource findRes(Resource res) {
        if (res != null) {
            String resName = res.getName();
            if (membersResource.getChild(resName) != null
                && (res.getPath()).equals(ResourceUtil.getValueMap(
                    membersResource.getChild(resName)).get(ResourceCollectionConstants.REF_PROPERTY, "")))
                return membersResource.getChild(resName);
            // handle multiple res with same name but different paths
            Iterator<Resource> children = membersResource.listChildren();
            while (children.hasNext()) {
                Resource r = children.next();
                if (ResourceUtil.getValueMap(r).get(ResourceCollectionConstants.REF_PROPERTY, "").equals(
                    res.getPath())) return r;
            }
        }
        return null;
    }

   	public void orderBefore(Resource srcResource, Resource destResource) {
		if (srcResource == null) {
			throw new IllegalArgumentException("Source Resource can not be null");
		}
		ModifiableValueMap vm = membersResource.adaptTo(ModifiableValueMap.class);
    	String[] order = vm.get(ResourceCollectionConstants.REFERENCES_PROP, new String[]{});
    	String srcPath = srcResource.getPath();
		int srcIndex = ArrayUtils.indexOf(order, srcPath);
    	if (srcIndex < 0) {
    		log.warn("Collection ordering failed, as there is no resource {} in collection {} for destResource", 
    				srcPath, getPath());
    		return ; 
    	}
		if (destResource == null) {
			//add it to the end.
			order = (String[]) ArrayUtils.remove(order, srcIndex);
			order = (String[]) ArrayUtils.add(order, srcPath);
		} else {
			String destPath = destResource.getPath();
			
			if (destPath.equals(srcPath)) {
				String message = MessageFormat.format("Collection ordering failed, as source {0} and destination {1} can not be same", 
	    				srcPath, destPath);
				log.error(message);
				throw new IllegalArgumentException(message);
			}
			
			int destIndex = ArrayUtils.indexOf(order, destPath);
			
			if (destIndex < 0) {
				log.warn("Collection ordering failed, as there is no resource {} in collection {} for destResource", 
						destPath, getPath());
				return;
			}
			
			order = (String[]) ArrayUtils.remove(order, srcIndex);
			if (srcIndex < destIndex) { //recalculate dest index
				destIndex = ArrayUtils.indexOf(order, destPath);
			}
			order = (String[]) ArrayUtils.add(order, destIndex, srcPath);
		}
		
		vm.put(ResourceCollectionConstants.REFERENCES_PROP, order);
	}

	public ModifiableValueMap getProperties(Resource resource) {
		Iterator<Resource> entries = membersResource.listChildren();
        while (entries.hasNext()) {
        	Resource entry = entries.next();
        	String path = ResourceUtil.getValueMap(entry).get(
        			ResourceCollectionConstants.REF_PROPERTY, "");
            
            if (resource.getPath().equals(path)) {
            	return entry.adaptTo(ModifiableValueMap.class);
            }
        }
        
        return null;
	}
}
