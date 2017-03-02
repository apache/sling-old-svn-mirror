/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl.resourcemodel;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.validation.impl.model.ChildResourceImpl;
import org.apache.sling.validation.impl.model.ParameterizedValidatorImpl;
import org.apache.sling.validation.impl.model.ResourcePropertyImpl;
import org.apache.sling.validation.impl.model.ValidationModelBuilder;
import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ParameterizedValidator;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.model.spi.ValidationModelProvider;
import org.apache.sling.validation.spi.Validator;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//the event handler is dynamic and registered in the activate method
@Component(service = ValidationModelProvider.class)
public class ResourceValidationModelProviderImpl implements ValidationModelProvider, EventHandler {

    static final String MODEL_XPATH_QUERY = "/jcr:root%s/*[@sling:resourceType=\""
            + ResourceValidationModelProviderImpl.VALIDATION_MODEL_RESOURCE_TYPE + "\" and @"
            + ResourceValidationModelProviderImpl.VALIDATED_RESOURCE_TYPE + "=\"%s\"]";
    static final String[] TOPICS = { SlingConstants.TOPIC_RESOURCE_REMOVED, SlingConstants.TOPIC_RESOURCE_CHANGED,
            SlingConstants.TOPIC_RESOURCE_ADDED };

    public static final String NAME_REGEX = "nameRegex";
    public static final String CHILDREN = "children";
    public static final String VALIDATOR_ARGUMENTS = "validatorArguments";
    public static final String VALIDATORS = "validators";
    public static final String OPTIONAL = "optional";
    public static final String PROPERTY_MULTIPLE = "propertyMultiple";
    public static final String PROPERTIES = "properties";
    public static final String VALIDATION_MODEL_RESOURCE_TYPE = "sling/validation/model";
    public static final String APPLICABLE_PATHS = "applicablePaths";
    public static final String VALIDATED_RESOURCE_TYPE = "validatedResourceType";
    public static final String SEVERITY = "severity";

    @Reference
    ResourceResolverFactory rrf = null;

    private static final Logger LOG = LoggerFactory.getLogger(ResourceValidationModelProviderImpl.class);

    private ServiceRegistration<EventHandler> eventHandlerRegistration;

    @Reference
    private ServiceUserMapped serviceUserMapped;

    /** key = resource type of validation models value = a list of all validation models for the resource type given in the key */
    final Map<String, List<ValidationModel>> validationModelCacheByResourceType = new ConcurrentHashMap<>();

    @Activate
    protected void activate(ComponentContext componentContext) throws LoginException {
        ResourceResolver rr = null;
        try {
            rr = rrf.getServiceResourceResolver(null);
            StringBuilder sb = new StringBuilder("(");
            String[] searchPaths = rr.getSearchPath();
            if (searchPaths.length > 1) {
                sb.append("|");
            }
            for (String searchPath : searchPaths) {
                sb.append("(path=").append(searchPath + "*").append(")");
            }
            sb.append(")");
            Dictionary<String, Object> eventHandlerProperties = new Hashtable<String, Object>();
            eventHandlerProperties.put(EventConstants.EVENT_TOPIC, TOPICS);
            eventHandlerProperties.put(EventConstants.EVENT_FILTER, sb.toString());
            eventHandlerRegistration = componentContext.getBundleContext().registerService(
                    EventHandler.class, this, eventHandlerProperties);
            LOG.debug("Registered event handler for validation models in {}", sb.toString());
        } finally {
            if (rr != null) {
                rr.close();
            }
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        if (eventHandlerRegistration != null) {
            eventHandlerRegistration.unregister();
            eventHandlerRegistration = null;
        }
    }

    @Override
    public void handleEvent(Event event) {
        String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        if (path == null) {
            LOG.warn("Received event {}, but could not get the affected path", event);
            return;
        }
        Set<String> resourceTypesToInvalidate = new HashSet<>();
        switch (event.getTopic()) {
        case SlingConstants.TOPIC_RESOURCE_REMOVED:
            // find cache entries below the removed resource
            for (Entry<String, List<ValidationModel>> validationModelByResourceType : validationModelCacheByResourceType.entrySet()) {
                for (ValidationModel model : validationModelByResourceType.getValue()) {
                    if (model.getSource().startsWith(path)) {
                        LOG.debug("Invalidate validation model at {}, because resource at {} has been removed", model.getSource(), path);
                        resourceTypesToInvalidate.add(validationModelByResourceType.getKey());
                    }
                }
            }
            break;
        default:
            // only consider additions/changes of resources with resource type = validation model resource type
            String resourceType = (String) event.getProperty(SlingConstants.PROPERTY_RESOURCE_TYPE);
            if (resourceType == null) {
                LOG.warn("Received event {}, but could not get the modified/added resource type", event);
                return;
            }
            if (VALIDATION_MODEL_RESOURCE_TYPE.equals(resourceType)) {
                // retrieve the resource types covered by the newly added model
                String resourceTypeToInvalidate = null;
                try {
                    resourceTypeToInvalidate = getResourceTypeOfValidationModel(path);
                } catch (Exception e) {
                    LOG.warn("Could not get covered resource type of newly added validation model at " + path, e);
                }
                if (resourceTypeToInvalidate != null) {
                    LOG.debug("Invalidate validation models for resource type {}, because resource at {} provides a new/modified validation model for that type", resourceType, path);
                    resourceTypesToInvalidate.add(resourceTypeToInvalidate);
                } else {
                    LOG.debug("Resource at {} provides a new/modified validation model but could not yet determine for which resource type", path);
                }
            }
            // or paths already covered by the cache
            for (Entry<String, List<ValidationModel>> validationModelByResourceType : validationModelCacheByResourceType.entrySet()) {
                for (ValidationModel model : validationModelByResourceType.getValue()) {
                    if (path.startsWith(model.getSource())) {
                        LOG.debug("Invalidate validation model at {}, because resource below (at {}) has been modified", model.getSource(), path);
                        resourceTypesToInvalidate.add(validationModelByResourceType.getKey());
                    }
                }
            }
        }
        for (String resourceTypeToInvalidate : resourceTypesToInvalidate) {
            validationModelCacheByResourceType.remove(resourceTypeToInvalidate);
        }
    }

    private String getResourceTypeOfValidationModel(String path) throws LoginException {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = rrf.getServiceResourceResolver(null);
            Resource modelResource = resourceResolver.getResource(path);
            ValueMap properties = modelResource.adaptTo(ValueMap.class);
            if (properties == null) {
                throw new IllegalStateException("Could not adapt resource at " + path + " to a ValueMap");
            }
            return properties.get(VALIDATED_RESOURCE_TYPE, null);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.sling.validation.model.spi.ValidationModelProvider#getModels(java.lang.String, java.util.Map)
     */
    @Override
    public @Nonnull List<ValidationModel> getModels(@Nonnull String relativeResourceType, @Nonnull Map<String, Validator<?>> validatorsMap) {
        List<ValidationModel> cacheEntry = validationModelCacheByResourceType.get(relativeResourceType);
        if (cacheEntry == null) {
            cacheEntry = doGetModels(relativeResourceType, validatorsMap);
            validationModelCacheByResourceType.put(relativeResourceType, cacheEntry);
        } else {
            LOG.debug("Found entry in cache for resource type {}", relativeResourceType);
        }
        return cacheEntry;
    }
    
    /** 
     * Searches for validation models bound to a specific resource type through a search query. 
     *
     * @param relativeResourceType the resource type to look for
     * @param validatorsMap  all known validators in a map (key=id of validator). Only one of those should be used in the returned validation models.
     * @return a List of {@link ValidationModel}s. Never {@code null}, but might be empty collection in case no
     *         model for the given resource type could be found. Returns the models below "/apps" before the models below "/libs".
     * @throws IllegalStateException in case a validation model is found but it is invalid */
    @Nonnull
    private List<ValidationModel> doGetModels(@Nonnull String relativeResourceType, @Nonnull Map<String, Validator<?>> validatorsMap) {
        List<ValidationModel> validationModels = new ArrayList<ValidationModel>();
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = rrf.getServiceResourceResolver(null);
            String[] searchPaths = resourceResolver.getSearchPath();
            for (String searchPath : searchPaths) {
                final String queryString = String.format(MODEL_XPATH_QUERY, searchPath, relativeResourceType);
                LOG.debug("Looking for validation models with query '{}'", queryString);
                Iterator<Resource> models = resourceResolver.findResources(queryString, "xpath");
                while (models.hasNext()) {
                    Resource model = models.next();
                    LOG.debug("Found validation model resource {}.", model.getPath());
                    String resourcePath = model.getPath();
                    try {
                        ValidationModelBuilder modelBuilder = new ValidationModelBuilder();
                        ValueMap validationModelProperties = model.adaptTo(ValueMap.class);
                        modelBuilder.addApplicablePaths(validationModelProperties.get(ResourceValidationModelProviderImpl.APPLICABLE_PATHS, new String[]{}));
                        Resource propertiesResource = model.getChild(ResourceValidationModelProviderImpl.PROPERTIES);
                        modelBuilder.resourceProperties(buildProperties(validatorsMap, propertiesResource));
                        modelBuilder.childResources(buildChildren(model, model, validatorsMap));
                        ValidationModel vm = modelBuilder.build(relativeResourceType, resourcePath);
                        validationModels.add(vm);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Found invalid validation model in '" + resourcePath + "': "
                                + e.getMessage(), e);
                    }
                }
                if (!validationModels.isEmpty()) {
                    // do not continue to search in other search paths if some results were already found!
                    // earlier search paths overlay lower search paths (/apps wins over /libs)
                    // the applicable content paths do not matter here!
                    break;
                }
            }
            return validationModels;
        } catch (LoginException e) {
            throw new IllegalStateException("Could not get service resource resolver", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    /** Creates a set of the properties that a resource is expected to have, together with the associated validators.
     *
     * @param validatorsMap a map containing {@link Validator}s as values and their id's as keys
     * @param propertiesResource the resource identifying the properties node from a validation model's structure (might be {@code null})
     * @return a set of properties or an empty set if no properties are defined
     * @see ResourceProperty */
    private @Nonnull List<ResourceProperty> buildProperties(@Nonnull Map<String, Validator<?>> validatorsMap, Resource propertiesResource) {
        List<ResourceProperty> properties = new ArrayList<ResourceProperty>();
        if (propertiesResource != null) {
            for (Resource property : propertiesResource.getChildren()) {
                String fieldName = property.getName();
                ValueMap propertyValueMap = property.adaptTo(ValueMap.class);
                Boolean propertyMultiple = propertyValueMap.get(ResourceValidationModelProviderImpl.PROPERTY_MULTIPLE, false);
                Boolean propertyRequired = !propertyValueMap.get(ResourceValidationModelProviderImpl.OPTIONAL, false);
                String nameRegex = propertyValueMap.get(ResourceValidationModelProviderImpl.NAME_REGEX, null);
                Resource validators = property.getChild(ResourceValidationModelProviderImpl.VALIDATORS);
                List<ParameterizedValidator> parameterizedValidators = new ArrayList<ParameterizedValidator>();
                if (validators != null) {
                    Iterator<Resource> validatorsIterator = validators.listChildren();
                    while (validatorsIterator.hasNext()) {
                        Resource validatorResource = validatorsIterator.next();
                        ValueMap validatorProperties = validatorResource.adaptTo(ValueMap.class);
                        if (validatorProperties == null) {
                            throw new IllegalStateException("Could not adapt resource " + validatorResource.getPath() + " to ValueMap");
                        }
                        String validatorId = validatorResource.getName();
                        Validator<?> validator = validatorsMap.get(validatorId);
                        if (validator == null) {
                            throw new IllegalArgumentException("Could not find validator with id '" + validatorId + "'");
                        }
                        // get arguments for validator
                        String[] validatorArguments = validatorProperties.get(ResourceValidationModelProviderImpl.VALIDATOR_ARGUMENTS,
                                String[].class);
                        Map<String, Object> validatorArgumentsMap = new HashMap<String, Object>();
                        if (validatorArguments != null) {
                            for (String arg : validatorArguments) {
                                String[] keyValuePair = arg.split("=");
                                if (keyValuePair.length != 2) {
                                    continue;
                                }
                                validatorArgumentsMap.put(keyValuePair[0], keyValuePair[1]);
                            }
                        }
                        // get severity
                        Integer severity = validatorProperties.get(SEVERITY, Integer.class);
                        parameterizedValidators.add(new ParameterizedValidatorImpl(validator, validatorArgumentsMap, severity));
                    }
                }
                ResourceProperty f = new ResourcePropertyImpl(fieldName, nameRegex, propertyMultiple, propertyRequired,
                        parameterizedValidators);
                properties.add(f);
            }
        }
        return properties;
    }

    /** Searches children resources from a {@code modelResource}, starting from the {@code rootResource}. If one needs all the children
     * resources of a model, then the {@code modelResource} and the {@code rootResource} should be identical.
     *
     * @param modelResource the resource describing a {@link org.apache.sling.validation.api.ValidationModel}
     * @param rootResource the model's resource from which to search for children (this resource has to have a
     *            {@link ResourceValidationModelProviderImpl#CHILDREN} node directly underneath it)
     * @param validatorsMap a map containing {@link Validator}s as values and their class names as values
     * @return a list of all the children resources; the list will be empty if there are no children resources */
    private @Nonnull List<ChildResource> buildChildren(@Nonnull Resource modelResource, @Nonnull Resource rootResource,
            @Nonnull Map<String, Validator<?>> validatorsMap) {
        List<ChildResource> children = new ArrayList<ChildResource>();
        Resource childrenResource = rootResource.getChild(ResourceValidationModelProviderImpl.CHILDREN);
        if (childrenResource != null) {
            for (Resource child : childrenResource.getChildren()) {
                // if pattern is set, always use that
                ValueMap childrenProperties = child.adaptTo(ValueMap.class);
                if (childrenProperties == null) {
                    throw new IllegalStateException("Could not adapt resource " + child.getPath() + " to ValueMap");
                }
                final String name = child.getName();
                final String nameRegex;
                if (childrenProperties.containsKey(ResourceValidationModelProviderImpl.NAME_REGEX)) {
                    nameRegex = childrenProperties.get(ResourceValidationModelProviderImpl.NAME_REGEX, String.class);
                } else {
                    // otherwise fall back to the name
                    nameRegex = null;
                }
                boolean isRequired = !childrenProperties.get(ResourceValidationModelProviderImpl.OPTIONAL, false);
                ChildResource childResource = new ChildResourceImpl(name, nameRegex, isRequired,
                        buildProperties(validatorsMap, child.getChild(ResourceValidationModelProviderImpl.PROPERTIES)),
                        buildChildren(modelResource, child, validatorsMap));
                children.add(childResource);
            }
        }
        return children;
    }
}
