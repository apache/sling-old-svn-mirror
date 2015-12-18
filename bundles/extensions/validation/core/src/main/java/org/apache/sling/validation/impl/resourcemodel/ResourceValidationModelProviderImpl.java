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
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.validation.impl.model.ChildResourceImpl;
import org.apache.sling.validation.impl.model.ParameterizedValidatorImpl;
import org.apache.sling.validation.impl.model.ResourcePropertyImpl;
import org.apache.sling.validation.impl.model.ValidationModelImpl;
import org.apache.sling.validation.impl.util.Trie;
import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ParameterizedValidator;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.model.spi.ValidationModelCache;
import org.apache.sling.validation.model.spi.ValidationModelProvider;
import org.apache.sling.validation.spi.Validator;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//the event handler is dynamic and registered in the activate method
@Service(value = ValidationModelProvider.class)
@Component
public class ResourceValidationModelProviderImpl implements ValidationModelProvider, EventHandler {

    static final String MODEL_XPATH_QUERY = "/jcr:root%s/*[@sling:resourceType=\""+ResourceValidationModelProviderImpl.VALIDATION_MODEL_RESOURCE_TYPE+"\" and @"+ResourceValidationModelProviderImpl.VALIDATED_RESOURCE_TYPE+"=\"%s\"]";
    static final String[] TOPICS = { SlingConstants.TOPIC_RESOURCE_REMOVED, SlingConstants.TOPIC_RESOURCE_CHANGED,
            SlingConstants.TOPIC_RESOURCE_ADDED };

    @Reference
    private ResourceResolverFactory rrf = null;

    @Reference
    private ValidationModelCache cache;

    private static final Logger LOG = LoggerFactory.getLogger(ResourceValidationModelProviderImpl.class);

    @Reference
    private ThreadPoolManager tpm = null;

    private ThreadPool threadPool;

    private ServiceRegistration eventHandlerRegistration;
    public static final String NAME_REGEX = "nameRegex";
    public static final String CHILDREN = "children";
    public static final String VALIDATOR_ARGUMENTS = "validatorArguments";
    public static final String VALIDATORS = "validators";
    public static final String OPTIONAL = "optional";
    public static final String PROPERTY_MULTIPLE = "propertyMultiple";
    public static final String PROPERTY_TYPE = "propertyType";
    public static final String PROPERTIES = "properties";
    public static final String VALIDATION_MODEL_RESOURCE_TYPE = "sling/validation/model";
    public static final String MODELS_HOME = "validation/";
    public static final String APPLICABLE_PATHS = "applicablePaths";
    public static final String VALIDATED_RESOURCE_TYPE = "validatedResourceType";

    @Activate
    protected void activate(ComponentContext componentContext) throws LoginException {
        threadPool = tpm.get("Validation Service Thread Pool");
        ResourceResolver rr = null;
        try {
            rr = rrf.getAdministrativeResourceResolver(null);
            StringBuilder sb = new StringBuilder("(");
            String[] searchPaths = rr.getSearchPath();
            if (searchPaths.length > 1) {
                sb.append("|");
            }
            for (String searchPath : searchPaths) {
                if (searchPath.endsWith("/")) {
                    searchPath = searchPath.substring(0, searchPath.length() - 1);
                }
                String path = searchPath + "/*" + ResourceValidationModelProviderImpl.MODELS_HOME;
                sb.append("(path=").append(path).append("*)");
            }
            sb.append(")");
            Dictionary<String, Object> eventHandlerProperties = new Hashtable<String, Object>();
            eventHandlerProperties.put(EventConstants.EVENT_TOPIC, TOPICS);
            eventHandlerProperties.put(EventConstants.EVENT_FILTER, sb.toString());
            eventHandlerRegistration = componentContext.getBundleContext().registerService(
                    EventHandler.class.getName(), this, eventHandlerProperties);
            LOG.debug("Registered event handler for validation models in {}", sb.toString());
        } finally {
            if (rr != null) {
                rr.close();
            }
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        if (threadPool != null) {
            tpm.release(threadPool);
        }
        if (eventHandlerRegistration != null) {
            eventHandlerRegistration.unregister();
            eventHandlerRegistration = null;
        }
    }

    @Override
    public void handleEvent(Event event) {
        LOG.debug("Asynchronously invalidating models cache due to event {}", event);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                cache.invalidate();
            }
        };
        threadPool.execute(task);
    }

    /**
     * Searches for valid validation models in the JCR repository for a certain resource type. All validation models
     * will be returned in a {@link Trie} data structure for easy retrieval of the models using their
     * {@code applicable paths} as trie keys.
     * <p/>
     * A valid content-tree {@code ValidationModel} has the following structure:
     * 
     * <pre>
     * validationModel
     *      &#064;validatedResourceType
     *      &#064;applicablePaths = [path1,path2,...] (optional)
     *      &#064;sling:resourceType = sling/validation/model
     *      fields
     *          field1
     *              &#064;fieldType
     *              validators
     *                  validator1
     *                      &#064;validatorArguments = [key=value,key=value...] (optional)
     *                  validatorN
     *                      #064;validatorArguments = [key=value,key=value...] (optional)
     *          fieldN
     *              &#064;fieldType
     *              validators
     *                  validator1
     *                  &#064;validatorArguments = [key=value,key=value...] (optional)
     * </pre>
     *
     * @param relativeResourceType
     *            {@inheritDoc}
     * @param validatorsMap
     *            {@inheritDoc}
     * @return {@inheritDoc}
     * @throws {@inheritDoc}
     */

    @Override
    @Nonnull
    public Collection<ValidationModel> getModel(@Nonnull String relativeResourceType, @Nonnull Map<String, Validator<?>> validatorsMap, @Nonnull ResourceResolver resourceResolver) {
        ValidationModelImpl vm;
        Collection<ValidationModel> validationModels = new ArrayList<ValidationModel>();
        String[] searchPaths = resourceResolver.getSearchPath();
        for (String searchPath : searchPaths) {
            final String queryString = String.format(MODEL_XPATH_QUERY, searchPath, relativeResourceType);
            Iterator<Resource> models = resourceResolver.findResources(queryString, "xpath");
            while (models.hasNext()) {
                Resource model = models.next();
                LOG.debug("Found validation model resource {}.", model.getPath());
                String jcrPath = model.getPath();
                try {
                    ValueMap validationModelProperties = model.adaptTo(ValueMap.class);
                    String[] applicablePaths = PropertiesUtil.toStringArray(validationModelProperties.get(ResourceValidationModelProviderImpl.APPLICABLE_PATHS, String[].class));
                    Resource r = model.getChild(ResourceValidationModelProviderImpl.PROPERTIES);
                    List<ResourceProperty> resourceProperties = buildProperties(validatorsMap,r);
                    List<ChildResource> children = buildChildren(model, model, validatorsMap);
                    if (resourceProperties.isEmpty() && children.isEmpty()) {
                        throw new IllegalArgumentException("Neither children nor properties set.");
                    } else {
                        vm = new ValidationModelImpl(resourceProperties, relativeResourceType,
                                applicablePaths, children);
                        validationModels.add(vm);
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Found invalid validation model in '" + jcrPath + "': "
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
    }
    
    /**
     * Creates a set of the properties that a resource is expected to have, together with the associated validators.
     *
     * @param validatorsMap      a map containing {@link Validator}s as values and their class names as values
     * @param propertiesResource the resource identifying the properties node from a validation model's structure (might be {@code null})
     * @return a set of properties or an empty set if no properties are defined
     * @see ResourceProperty
     */
    private @Nonnull List<ResourceProperty> buildProperties(@Nonnull Map<String, Validator<?>> validatorsMap, Resource propertiesResource) {
        List<ResourceProperty> properties = new ArrayList<ResourceProperty>();
        if (propertiesResource != null) {
            for (Resource property : propertiesResource.getChildren()) {
                String fieldName = property.getName();
                ValueMap propertyValueMap = property.adaptTo(ValueMap.class);
                Boolean propertyMultiple = PropertiesUtil.toBoolean(propertyValueMap.get(ResourceValidationModelProviderImpl.PROPERTY_MULTIPLE), false);
                Boolean propertyRequired = !PropertiesUtil.toBoolean(propertyValueMap.get(ResourceValidationModelProviderImpl.OPTIONAL), false);
                String nameRegex = PropertiesUtil.toString(propertyValueMap.get(ResourceValidationModelProviderImpl.NAME_REGEX), null);
                Resource validators = property.getChild(ResourceValidationModelProviderImpl.VALIDATORS);
                List<ParameterizedValidator> parameterizedValidators = new ArrayList<ParameterizedValidator>();
                if (validators != null) {
                    Iterator<Resource> validatorsIterator = validators.listChildren();
                    while (validatorsIterator.hasNext()) {
                        Resource validator = validatorsIterator.next();
                        ValueMap validatorProperties = validator.adaptTo(ValueMap.class);
                        if (validatorProperties == null) {
                            throw new IllegalStateException("Could not adapt resource " + validator.getPath() + " to ValueMap");
                        }
                        String validatorName = validator.getName();
                        Validator<?> v = validatorsMap.get(validatorName);
                        if (v == null) {
                            throw new IllegalArgumentException("Could not find validator with name '" + validatorName + "'");
                        }
                        // get type of validator
                        String[] validatorArguments = validatorProperties.get(ResourceValidationModelProviderImpl.VALIDATOR_ARGUMENTS, String[].class);
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
                        parameterizedValidators.add(new ParameterizedValidatorImpl(v, validatorArgumentsMap));
                    }
                }
                ResourceProperty f = new ResourcePropertyImpl(fieldName, nameRegex, propertyMultiple, propertyRequired, parameterizedValidators);
                properties.add(f);
            }
        }
        return properties;
    }

    /**
     * Searches children resources from a {@code modelResource}, starting from the {@code rootResource}. If one needs all the children
     * resources of a model, then the {@code modelResource} and the {@code rootResource} should be identical.
     *
     * @param modelResource          the resource describing a {@link org.apache.sling.validation.api.ValidationModel}
     * @param rootResource           the model's resource from which to search for children (this resource has to have a {@link
     *                               ResourceValidationModelProviderImpl#CHILDREN} node directly underneath it)
     * @param validatorsMap          a map containing {@link Validator}s as values and their class names as values
     * @return a list of all the children resources; the list will be empty if there are no children resources
     */
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
                boolean isRequired = !PropertiesUtil.toBoolean(childrenProperties.get(ResourceValidationModelProviderImpl.OPTIONAL), false);
                ChildResource childResource = new ChildResourceImpl(name, nameRegex, isRequired, buildProperties(validatorsMap, child.getChild(ResourceValidationModelProviderImpl.PROPERTIES)), buildChildren(modelResource, child, validatorsMap));
                children.add(childResource);
            }
        }
        return children;
    }
}
