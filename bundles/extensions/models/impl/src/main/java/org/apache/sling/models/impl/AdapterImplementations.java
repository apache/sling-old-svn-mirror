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
package org.apache.sling.models.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.impl.model.ModelClass;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects alternative adapter implementations that may be defined in a @Model.adapters attribute.
 * If multiple models implement the same adapter they are all collected and can be chose via a ImplementationPicker.
 * Additionally it acts as a cache for model classes without adapter definitions, where adapter and implementation type is the same.
 * The implementation is thread-safe.
 */
final class AdapterImplementations {

    private static final Logger log = LoggerFactory.getLogger(AdapterImplementations.class);

    private final ConcurrentMap<String,ConcurrentNavigableMap<String,ModelClass<?>>> adapterImplementations
            = new ConcurrentHashMap<String,ConcurrentNavigableMap<String,ModelClass<?>>>();

    private final ConcurrentMap<String,ModelClass<?>> modelClasses
            = new ConcurrentHashMap<String,ModelClass<?>>();
    
    private final ConcurrentMap<String, Class<?>> resourceTypeMappingsForResources = new ConcurrentHashMap<String, Class<?>>();
    private final ConcurrentMap<String, Class<?>> resourceTypeMappingsForRequests = new ConcurrentHashMap<String, Class<?>>();
    private final ConcurrentMap<Bundle, List<String>> resourceTypeRemovalListsForResources = new ConcurrentHashMap<Bundle, List<String>>();
    private final ConcurrentMap<Bundle, List<String>> resourceTypeRemovalListsForRequests = new ConcurrentHashMap<Bundle, List<String>>();

    private volatile ImplementationPicker[] sortedImplementationPickers = new ImplementationPicker[0];
    private volatile StaticInjectAnnotationProcessorFactory[] sortedStaticInjectAnnotationProcessorFactories = new StaticInjectAnnotationProcessorFactory[0];

    public void setImplementationPickers(Collection<ImplementationPicker> implementationPickers) {
        this.sortedImplementationPickers = implementationPickers.toArray(new ImplementationPicker[implementationPickers.size()]);
    }

    public ImplementationPicker[] getImplementationPickers() {
        return this.sortedImplementationPickers;
    }
    
    public StaticInjectAnnotationProcessorFactory[] getStaticInjectAnnotationProcessorFactories() {
        return sortedStaticInjectAnnotationProcessorFactories;
    }

    public void setStaticInjectAnnotationProcessorFactories(
            Collection<StaticInjectAnnotationProcessorFactory> factories) {
        this.sortedStaticInjectAnnotationProcessorFactories = factories.toArray(new StaticInjectAnnotationProcessorFactory[factories.size()]);
        updateProcessorFactoriesInModelClasses();
    }
    
    /**
     * Updates all {@link ModelClass} instances with updates list of static inject annotation processor factories.
     */
    private void updateProcessorFactoriesInModelClasses() {
        Iterator<ModelClass<?>> items = modelClasses.values().iterator();
        updateProcessorFactoriesInModelClasses(items);        
        Iterator<ConcurrentNavigableMap<String,ModelClass<?>>> mapItems = adapterImplementations.values().iterator();
        while (mapItems.hasNext()) {
            ConcurrentNavigableMap<String,ModelClass<?>> mapItem = mapItems.next();
            updateProcessorFactoriesInModelClasses(mapItem.values().iterator());
        }
    }
    private void updateProcessorFactoriesInModelClasses(Iterator<ModelClass<?>> items) {
        while (items.hasNext()) {
            ModelClass<?> item = items.next();
            item.updateProcessorFactories(sortedStaticInjectAnnotationProcessorFactories);
        }
    }
    
    /** Add implementation mapping for the given model class (implementation is the model class itself).
     * Only used for testing purposes. Use {@link #add(Class, Class)} in case you want to register a different implementation.
     * @param modelClasses the model classes to register
     */
    protected void addClassesAsAdapterAndImplementation(Class<?>... modelClasses) {
        for (Class<?> modelClass : modelClasses) {
            add(modelClass, modelClass);
        }
    }
    
    /**
     * Add implementation mapping for the given adapter type.
     * @param adapterType Adapter type
     * @param implType Implementation type
     */
    @SuppressWarnings("unchecked")
    public void add(Class<?> adapterType, Class<?> implType) {
        String key = adapterType.getName();
        if (adapterType == implType) {
            modelClasses.put(key, new ModelClass(implType, sortedStaticInjectAnnotationProcessorFactories));
        }
        else {
            // although we already use a ConcurrentMap synchronize explicitly because we apply non-atomic operations on it
            synchronized (adapterImplementations) {
                ConcurrentNavigableMap<String,ModelClass<?>> implementations = adapterImplementations.get(key);
                if (implementations == null) {
                    // to have a consistent ordering independent of bundle loading use a ConcurrentSkipListMap that sorts by class name
                    implementations = new ConcurrentSkipListMap<String,ModelClass<?>>();
                    adapterImplementations.put(key, implementations);
                }
                implementations.put(implType.getName(), new ModelClass(implType, sortedStaticInjectAnnotationProcessorFactories));
            }
        }
    }
    
    /**
     * Remove implementation mapping for the given adapter type.
     * @param adapterTypeName Adapter type name
     * @param implTypeName Implementation type name
     */
    public void remove(String adapterTypeName, String implTypeName) {
        String key = adapterTypeName;
        if (StringUtils.equals(adapterTypeName, implTypeName)) {
            modelClasses.remove(key);
        }
        else {
            // although we already use a ConcurrentMap synchronize explicitly because we apply non-atomic operations on it
            synchronized (adapterImplementations) {
                ConcurrentNavigableMap<String,ModelClass<?>> implementations = adapterImplementations.get(key);
                if (implementations != null) {
                    implementations.remove(implTypeName);
                    if (implementations.isEmpty()) {
                        adapterImplementations.remove(key);
                    }
                }
            }
        }
    }

    /**
     * Remove all implementation mappings.
     */
    public void removeAll() {
        modelClasses.clear();
        adapterImplementations.clear();
    }

    /**
     * Lookup the best-matching implementation for the given adapter type by enquiring the {@link ImplementationPicker} services.
     * @param adapterType Adapter type
     * @param adaptable Adaptable for reference
     * @return Implementation type or null if none detected
     */
    @SuppressWarnings("unchecked")
    public <ModelType> ModelClass<ModelType> lookup(Class<ModelType> adapterType, Object adaptable) {
        String key = adapterType.getName();
        
        // lookup in cache for models without adapter classes
        ModelClass<ModelType> modelClass = (ModelClass<ModelType>)modelClasses.get(key);
        if (modelClass!=null) {
            return modelClass;
        }

        // not found? look in cache with adapter classes
        ConcurrentNavigableMap<String,ModelClass<?>> implementations = adapterImplementations.get(key);
        if (implementations==null || implementations.isEmpty()) {
            return null;
        }
        Collection<ModelClass<?>> implementationsCollection = implementations.values();
        ModelClass<?>[] implementationWrappersArray = implementationsCollection.toArray(new ModelClass<?>[implementationsCollection.size()]);
        
        // prepare array for implementation picker
        Class<?>[] implementationsArray = new Class<?>[implementationsCollection.size()];
        for (int i=0; i<implementationWrappersArray.length; i++) {
            implementationsArray[i] = implementationWrappersArray[i].getType();
        }

        for (ImplementationPicker picker : this.sortedImplementationPickers) {
            Class<?> implementation = picker.pick(adapterType, implementationsArray, adaptable);
            if (implementation != null) {
                for (int i=0; i<implementationWrappersArray.length; i++) {
                    if (implementation==implementationWrappersArray[i].getType()) {
                        return (ModelClass<ModelType>)implementationWrappersArray[i];
                    }
                }
            }
        }

        return null;
    }

    /**
     * @param adapterType the type to check
     * @return {@code true} in case the given type is a model (may be with a different adapter class)
     */
    @SuppressWarnings("unchecked")
    public <ModelType> boolean isModelClass(Class<ModelType> adapterType) {
        String key = adapterType.getName();
        
        // lookup in cache for models without adapter classes
        ModelClass<ModelType> modelClass = (ModelClass<ModelType>)modelClasses.get(key);
        if (modelClass!=null) {
            return true;
        }

        // not found? look in cache with adapter classes
        ConcurrentNavigableMap<String,ModelClass<?>> implementations = adapterImplementations.get(key);
        if (implementations==null || implementations.isEmpty()) {
            return false;
        }
        return true;
    }

     public void registerModelToResourceType(final Bundle bundle, final String resourceType, final Class<?> adaptableType, final Class<?> clazz) {
         if (resourceType.startsWith("/")) {
             log.warn("Registering model class {} for adaptable {} with absolute resourceType {}." ,
                     new Object[] { clazz, adaptableType, resourceType });
         }
         ConcurrentMap<String, Class<?>> map;
         ConcurrentMap<Bundle, List<String>> resourceTypeRemovalLists;
         if (adaptableType == Resource.class) {
             map = resourceTypeMappingsForResources;
             resourceTypeRemovalLists = resourceTypeRemovalListsForResources;
         } else if (adaptableType == SlingHttpServletRequest.class) {
             map = resourceTypeMappingsForRequests;
             resourceTypeRemovalLists = resourceTypeRemovalListsForRequests;
         } else {
             log.warn("Found model class {} with resource type {} for adaptable {}. Unsupported type for resourceType binding.",
                     new Object[] { clazz, resourceType, adaptableType });
             return;
         }
         Class<?> existingMapping = map.putIfAbsent(resourceType, clazz);
         if (existingMapping == null) {
             resourceTypeRemovalLists.putIfAbsent(bundle, new CopyOnWriteArrayList<String>());
             resourceTypeRemovalLists.get(bundle).add(resourceType);
         } else {
             log.warn("Skipped registering {} for resourceType {} under adaptable {} because of existing mapping to {}",
                     new Object[] { clazz, resourceType, adaptableType, existingMapping });
         }
     }

     public void removeResourceTypeBindings(final Bundle bundle) {
         List<String> registeredResourceTypes = resourceTypeRemovalListsForResources.remove(bundle);
         if (registeredResourceTypes != null) {
             for (String resourceType : registeredResourceTypes) {
                 resourceTypeMappingsForResources.remove(resourceType);
             }
         }
         registeredResourceTypes = resourceTypeRemovalListsForRequests.remove(bundle);
         if (registeredResourceTypes != null) {
             for (String resourceType : registeredResourceTypes) {
                 resourceTypeMappingsForRequests.remove(resourceType);
             }
         }
     }

     public Class<?> getModelClassForRequest(final SlingHttpServletRequest request) {
         return getModelClassForResource(request.getResource(), resourceTypeMappingsForRequests);
     }

    public Class<?> getModelClassForResource(final Resource resource) {
        return getModelClassForResource(resource, resourceTypeMappingsForResources);
    }

    protected static Class<?> getModelClassForResource(final Resource resource, final Map<String, Class<?>> map) {
        if (resource == null) {
            return null;
        }
        ResourceResolver resolver = resource.getResourceResolver();
        final String originalResourceType = resource.getResourceType();
        Class<?> modelClass = getClassFromResourceTypeMap(originalResourceType, map, resolver);
        if (modelClass != null) {
            return modelClass;
        } else {
            String resourceType = resolver.getParentResourceType(resource);
            while (resourceType != null) {
                modelClass = getClassFromResourceTypeMap(resourceType, map, resolver);
                if (modelClass != null) {
                    return modelClass;
                } else {
                    resourceType = resolver.getParentResourceType(resourceType);
                }
            }
            Resource resourceTypeResource = resolver.getResource(originalResourceType);
            return getModelClassForResource(resourceTypeResource, map);
        }
    }

    private static Class<?> getClassFromResourceTypeMap(final String resourceType, final Map<String, Class<?>> map, final ResourceResolver resolver) {
        Class<?> modelClass = map.get(resourceType);
        if (modelClass == null) {
            for (String searchPath : resolver.getSearchPath()) {
                if (resourceType.startsWith("/")) {
                    if (resourceType.startsWith(searchPath)) {
                        modelClass = map.get(resourceType.substring(searchPath.length()));
                        if (modelClass != null) {
                            break;
                        }
                    }
                } else {
                    modelClass = map.get(searchPath + resourceType);
                    if (modelClass != null) {
                        break;
                    }
                }
            }
        }
        return modelClass;
    }
}
