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

package org.apache.sling.resource.collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;

/**
 * ResourceCollection is an ordered collection of {@link Resource}. 
 * The collection does not store the actual Resources, it only points to
 * them. 
 * 
 * Each entry in the collection is represented by a Resource which contains a 
 * reference to original resource. That reference Resource can have additional 
 * properties (creationDate, etc.)
 */
public interface ResourceCollection {
    
	/**
	 * Sling resource type for resource representing a </code>ResourceCollection<code>
	 */
    public static final String RESOURCE_TYPE = "sling/collection";
    
    /**
     * Returns name of the collection.
     * 
     * @return name of the collection.
     */
    public String getName();
    
    /**
     * Returns path of the collection.
     * 
     * @return path of the collection.
     */
    public String getPath();
	
    /**
     * Returns an iterator over resources referred in the collection in the specified order.
     *
     * @return iterator over resources referred in collection.
     */
    Iterator<Resource> getResources();
    
    /**
     * Returns additional properties for a particular resource in Collection entry.
     * 
     * @return properties of the Collection entry as <code>ModifiableValueMap</code>, returns null if entry found.
     */
    ModifiableValueMap getProperties(Resource resource);
    
    /**
     * Returns true if resource reference is part of the collection.
     * 
     * @param resource resource to be checked
     * @return true if resource is part of the collection.
     *         false otherwise
     */
    boolean contains(Resource resource);
    
    /**
     * Creates a new entry in the collection at the last position and add a reference to resource 
     * in the entry.  
     * Changes are transient & have to be saved by calling resolver.commit()
     * 
     * @param resource resource to be added
     * @param properties The additional properties to be stored with the collection entry (can be null).
     * @return true if addition of resource to collection was successful or 
     *         false if collection already contained the resource or resource is null.
     * 
     * @throws {@link PersistenceException} if the operation fails
     */
    boolean add(Resource resource, Map<String, Object> properties) throws PersistenceException;
    
    /**
     * Creates a new entry in the collection at the last position and add a reference to resource 
     * in the entry.  
     * Changes are transient & have to be saved by calling resolver.commit()
     * 
     * @param resource resource to be added
     * @return true if addition of resource to collection was successful or 
     *         false if collection already contained the resource or resource is null.
     * 
     * @throws {@link PersistenceException} if the operation fails
     */
    boolean add(Resource resource) throws PersistenceException;
    
      
    /**
     * Removes a entry of resource from collection & returns true if successful. 
     * Changes are transient & have to be saved by calling resolver.commit()
     * 
     * @param resource resource reference to be removed
     * @return true if resource reference was successfully removed from the collection.
     *         false if not removed/not present
     * 
     * @throws {@link PersistenceException} if the operation fails
     */
    boolean remove(Resource resource) throws PersistenceException; 
    
    /**
     * This method inserts the referenced resource <code>srcResource</code>
     * into the collection entry at the position immediately before the referenced resource <code>destResource</code>. 
     * 
     * To insert the referenced resource into last position, <code>destResource</code> can be null.
     * 
     * @param srcResource Referenced resource that needs to be moved in the order
     * @param destResource Referenced resource before which the <code>srcResource</code> will be placed.
     */
    void orderBefore(Resource srcResource, Resource destResource);
}
