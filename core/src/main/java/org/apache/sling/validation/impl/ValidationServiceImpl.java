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
package org.apache.sling.validation.impl;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
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
import org.apache.sling.validation.api.ChildResource;
import org.apache.sling.validation.api.ParameterizedValidator;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.ValidationModel;
import org.apache.sling.validation.api.ValidationResult;
import org.apache.sling.validation.api.ValidationService;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.api.exceptions.SlingValidationException;
import org.apache.sling.validation.impl.util.JCRBuilder;
import org.apache.sling.validation.impl.util.Trie;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component()
@Service(ValidationService.class)
public class ValidationServiceImpl implements ValidationService, EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationServiceImpl.class);

    static final String MODEL_XPATH_QUERY = "/jcr:root%s/" + Constants.MODELS_HOME + "/*[@sling:resourceType=\"%s\" and @%s=\"%s\"]";
    static final String[] TOPICS = {SlingConstants.TOPIC_RESOURCE_REMOVED, SlingConstants.TOPIC_RESOURCE_CHANGED,
            SlingConstants.TOPIC_RESOURCE_ADDED};

    protected Map<String, Trie<JCRValidationModel>> validationModelsCache = new ConcurrentHashMap<String, Trie<JCRValidationModel>>();
    private ThreadPool threadPool;
    private ServiceRegistration eventHandlerRegistration;

    @Reference
    private ResourceResolverFactory rrf = null;

    @Reference(
            name = "validator",
            referenceInterface = Validator.class,
            policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
    )
    Map<String, Validator<?>> validators = new ConcurrentHashMap<String, Validator<?>>();

    @Reference
    private ThreadPoolManager tpm = null;

    // ValidationService ###################################################################################################################
    @Override
    public ValidationModel getValidationModel(String validatedResourceType, String resourcePath) {
        if (resourcePath == null) {
            throw new IllegalArgumentException("ValidationService.getValidationModel - cannot accept null as resource path");
        }
        if (validatedResourceType == null) {
            throw new IllegalArgumentException("ValidationService.getValidationModel - cannot accept null as resource type. Resource path was: " + resourcePath);
        }
        validatedResourceType = getRelativeResourceType(validatedResourceType);
        ValidationModel model = null;
        Trie<JCRValidationModel> modelsForResourceType = validationModelsCache.get(validatedResourceType);
        if (modelsForResourceType != null) {
            model = modelsForResourceType.getElementForLongestMatchingKey(resourcePath).getValue();
        }
        if (model == null) {
            modelsForResourceType = searchAndStoreValidationModel(validatedResourceType);
            if (modelsForResourceType != null) {
                model = modelsForResourceType.getElementForLongestMatchingKey(resourcePath).getValue();
                if (model == null) {
                    LOG.warn("Although model for resource type {} is available, it is not allowed for path {}", validatedResourceType, resourcePath);
                }
            }
            
        }
        return model;
    }

    @Override
    public ValidationModel getValidationModel(Resource resource) {
        return getValidationModel(resource.getResourceType(), resource.getPath());
    }

    @Override
    public ValidationResult validate(Resource resource, ValidationModel model) {
        return validate(resource, model, "");
    }
    
    protected ValidationResult validate(Resource resource, ValidationModel model, String relativePath) {
        if (resource == null || model == null) {
            throw new IllegalArgumentException("ValidationService.validate - cannot accept null parameters");
        }
        ValidationResultImpl result = new ValidationResultImpl();

        // validate direct properties of the resource
        validateValueMap(resource.adaptTo(ValueMap.class), relativePath, model.getResourceProperties(), result );

        // validate children resources, if any
        validateChildren(resource, relativePath, model.getChildren(), result);
        return result;
    }

    /**
     * If the given resourceType is starting with a "/", it will strip out the leading search path from the given resource type.
     * Otherwise it will just return the given resource type (as this is already relative).
     * @param resourceType
     * @return a relative resource type (without the leading search path)
     * @throws IllegalArgumentException in case the resource type is starting with a "/" but not with any of the search paths.
     */
    protected String getRelativeResourceType(String resourceType) throws IllegalArgumentException {
        if (resourceType.startsWith("/")) {
            LOG.debug("try to strip the search path from the resource type");
            ResourceResolver rr = null;
            try {
                rr = rrf.getAdministrativeResourceResolver(null);
                for (String searchPath : rr.getSearchPath()) {
                    if (resourceType.startsWith(searchPath)) {
                        resourceType = resourceType.substring(searchPath.length());
                        return resourceType;
                    }
                }
                throw new IllegalArgumentException("Can only deal with resource types inside the resource resolver's search path (" + StringUtils.join(rr.getSearchPath()) 
                        + ") but given resource type " + resourceType + " is outside!");
            } catch (LoginException e) {
                throw new IllegalStateException("Could not login as administrator to figure out search paths", e);
            } finally {
                if (rr != null) {
                    rr.close();
                }
            }
        }
        return resourceType;
        
    }

    /**
     * Validates a child resource with the help of the given {@code ChildResource} entry from the validation model
     * @param resource
     * @param relativePath relativePath of the resource (must be empty or end with "/")
     * @param result
     * @param childResources
     */
    private void validateChildren(Resource resource, String relativePath, List<ChildResource> childResources, ValidationResultImpl result) {
        // validate children resources, if any
        for (ChildResource childResource : childResources) {
            // if a pattern is set we validate all children matching that pattern
            if (childResource.getNamePattern() != null) {
                boolean foundMatch = false;
                for (Resource child : resource.getChildren()) {
                    Matcher matcher = childResource.getNamePattern().matcher(child.getName());
                    if (matcher.matches()) {
                       validateChildResource(child, relativePath, childResource, result);
                       foundMatch = true;
                    }
                }
                if (!foundMatch && childResource.isRequired()) {
                    result.addFailureMessage(relativePath + childResource.getNamePattern().pattern(), "Missing required child resource.");
                }
            } else {
                Resource expectedResource = resource.getChild(childResource.getName());
                if (expectedResource != null) {
                    validateChildResource(expectedResource, relativePath, childResource, result);
                } else if (childResource.isRequired()) {
                    result.addFailureMessage(relativePath + childResource.getName(), "Missing required child resource.");
                }
            } 
        }
    }
    
    private void validateChildResource(Resource resource, String relativePath, ChildResource childResource, ValidationResultImpl result) {
        validateValueMap(resource.adaptTo(ValueMap.class), relativePath + resource.getName() + "/", childResource.getProperties(), result);
        validateChildren(resource, relativePath + resource.getName() + "/", childResource.getChildren(), result);
    }

    @Override
    public ValidationResult validate(ValueMap valueMap, ValidationModel model) {
        if (valueMap == null || model == null) {
            throw new IllegalArgumentException("ValidationResult.validate - cannot accept null parameters");
        }
        ValidationResultImpl result = new ValidationResultImpl();
        validateValueMap(valueMap,  "", model.getResourceProperties(), result);
        return result;
    }    

    @Override
    public ValidationResult validateAllResourceTypesInResource(Resource resource, boolean enforceValidation, Set<String> ignoredResourceTypes)
            throws IllegalStateException, IllegalArgumentException, SlingValidationException {
        if (ignoredResourceTypes == null) {
            ignoredResourceTypes = Collections.emptySet();
        }
        ValidationResourceVisitor visitor = new ValidationResourceVisitor(this, resource.getPath(), enforceValidation, ignoredResourceTypes);
        visitor.accept(resource);
        return visitor.getResult();
    }

    // EventHandler ########################################################################################################################
    @Override
    public void handleEvent(Event event) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                validationModelsCache.clear();
            }
        };
        threadPool.execute(task);
    }

    // OSGi ################################################################################################################################
    @SuppressWarnings("unused")
    protected void activate(ComponentContext componentContext) {
        threadPool = tpm.get("Validation Service Thread Pool");
        ResourceResolver rr = null;
        try {
            rr = rrf.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            LOG.error("Cannot obtain a resource resolver.");
        }
        if (rr != null) {
            StringBuilder sb = new StringBuilder("(");
            String[] searchPaths = rr.getSearchPath();
            if (searchPaths.length > 1) {
                sb.append("|");
            }
            for (String searchPath : searchPaths) {
                if (searchPath.endsWith("/")) {
                    searchPath = searchPath.substring(0, searchPath.length() - 1);
                }
                String path = searchPath + "/*" + Constants.MODELS_HOME;
                sb.append("(path=").append(path).append("*)");
            }
            sb.append(")");
            Dictionary<String, Object> eventHandlerProperties = new Hashtable<String, Object>();
            eventHandlerProperties.put(EventConstants.EVENT_TOPIC, TOPICS);
            eventHandlerProperties.put(EventConstants.EVENT_FILTER, sb.toString());
            eventHandlerRegistration = componentContext.getBundleContext().registerService(EventHandler.class.getName(), this,
                    eventHandlerProperties);
            rr.close();
        } else {
            LOG.warn("Null resource resolver. Cannot apply path filtering for event processing. Skipping registering this service as an " +
                    "EventHandler");
        }
    }

    @SuppressWarnings("unused")
    protected void deactivate(ComponentContext componentContext) {
        if (threadPool != null) {
            tpm.release(threadPool);
        }
        if (eventHandlerRegistration != null) {
            eventHandlerRegistration.unregister();
            eventHandlerRegistration = null;
        }
    }

    private void validateValueMap(ValueMap valueMap, String relativePath, Set<ResourceProperty> resourceProperties,
            ValidationResultImpl result) {
        if (valueMap == null) {
            throw new IllegalArgumentException("ValueMap may not be null");
        }
        for (ResourceProperty resourceProperty : resourceProperties) {
            if (resourceProperty.getNamePattern() != null) {
                boolean foundMatch = false;
                for (String key : valueMap.keySet()) {
                    if (resourceProperty.getNamePattern().matcher(key).matches()) {
                        foundMatch = true;
                        validateValueMap(key, valueMap, relativePath, resourceProperty, result);
                    }
                }
                if (!foundMatch && resourceProperty.isRequired()) {
                    result.addFailureMessage(relativePath + resourceProperty.getNamePattern(), "Missing required property.");
                }
            } else {
                validateValueMap(resourceProperty.getName(), valueMap, relativePath, resourceProperty, result);
            }
        }
    }
    
    
    private void validateValueMap(String property, ValueMap valueMap, String relativePath, ResourceProperty resourceProperty, ValidationResultImpl result) {
        Object fieldValues = valueMap.get(property);
        if (fieldValues == null) {
            if (resourceProperty.isRequired()) {
                result.addFailureMessage(relativePath + property, "Missing required property.");
            }
            return;
        }
        List<ParameterizedValidator> validators = resourceProperty.getValidators();
        if (resourceProperty.isMultiple()) {
            if (!fieldValues.getClass().isArray()) {
                result.addFailureMessage(relativePath + property, "Expected multiple-valued property.");
                return;
            }
        }
        validatePropertyValue(result, property, relativePath, valueMap, validators);
    }

    /**
     * Searches for valid validation models in the JCR repository for a certain resource type. All validation models will be returned in a
     * {@link Trie} data structure for easy retrieval of the models using their {@code applicable paths} as trie keys.
     * <p/>
     * A valid content-tree {@code ValidationModel} has the following structure:
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
     * @param validatedResourceType the type of resource for which to scan the JCR repository for validation models
     * @return a {@link Trie} with the validation models; an empty trie if no model is found
     */
    private Trie<JCRValidationModel> searchAndStoreValidationModel(String validatedResourceType) {
        Trie<JCRValidationModel> modelsForResourceType = null;
        ResourceResolver rr = null;
        JCRValidationModel vm;
        if (StringUtils.isBlank(validatedResourceType)) {
            throw new IllegalArgumentException("validatedResourceType cannot be null or blank!");
        }
        try {
            rr = rrf.getAdministrativeResourceResolver(null);
            String[] searchPaths = rr.getSearchPath();
            for (String searchPath : searchPaths) {
                final String queryString = String.format(MODEL_XPATH_QUERY, searchPath, Constants.VALIDATION_MODEL_RESOURCE_TYPE,
                        Constants.VALIDATED_RESOURCE_TYPE, validatedResourceType);
                Iterator<Resource> models = rr.findResources(queryString, "xpath");
                while (models.hasNext()) {
                    Resource model = models.next();
                    LOG.info("Found validation model resource {}.", model.getPath());
                    String jcrPath = model.getPath();
                    try {
                        ValueMap validationModelProperties = model.adaptTo(ValueMap.class);
                        String[] applicablePaths = PropertiesUtil.toStringArray(validationModelProperties.get(Constants.APPLICABLE_PATHS,
                                String[].class));
                        Resource r = model.getChild(Constants.PROPERTIES);
                        Set<ResourceProperty> resourceProperties = JCRBuilder.buildProperties(validators, r);
                        List<ChildResource> children = JCRBuilder.buildChildren(model, model, validators);
                        if (resourceProperties.isEmpty() && children.isEmpty()) {
                            throw new IllegalArgumentException("Neither children nor properties set.");
                        } else {
                            vm = new JCRValidationModel(jcrPath, resourceProperties, validatedResourceType, applicablePaths, children);
                            modelsForResourceType = validationModelsCache.get(validatedResourceType);
                            /**
                             * if the modelsForResourceType is null the canAcceptModel will return true: performance
                             * optimisation so that the Trie is created only if the model is accepted
                             */

                            if (canAcceptModel(vm, searchPath, searchPaths, modelsForResourceType)) {
                                if (modelsForResourceType == null) {
                                    modelsForResourceType = new Trie<JCRValidationModel>();
                                    validationModelsCache.put(validatedResourceType, modelsForResourceType);
                                }
                                for (String applicablePath : vm.getApplicablePaths()) {
                                    modelsForResourceType.insert(applicablePath, vm);
                                }
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Found invalid validation model in '" + jcrPath +"': " + e.getMessage(), e);
                    }
                }
            }
        } catch (LoginException e) {
            throw new IllegalStateException("Unable to obtain a resource resolver.", e);
        } finally {
            if (rr != null) {
                rr.close();
            }
        }
        return modelsForResourceType;
    }

    /**
     * Checks if the {@code validationModel} does not override an existing stored model given the fact that the overlaying is done based on
     * the order in which the search paths are in the {@code searchPaths} array: the lower the index, the higher the priority.
     *
     * @param validationModel   the model to be checked
     * @param currentSearchPath the current search path
     * @param searchPaths       the available search paths
     * @param validationModels  the existing validation models
     * @return {@code true} if the new model can be stored, {@code false} otherwise
     */
    private boolean canAcceptModel(JCRValidationModel validationModel, String currentSearchPath, String[] searchPaths,
                                   Trie<JCRValidationModel> validationModels) {
        // perform null check to optimise performance in callee - no need to previously create the Trie if we're not going to accept the model
        if (validationModels != null) {
            String relativeModelPath = validationModel.getJcrPath().replaceFirst(currentSearchPath, "");
            for (String searchPath : searchPaths) {
                if (!currentSearchPath.equals(searchPath)) {
                    for (String applicablePath : validationModel.getApplicablePaths()) {
                        JCRValidationModel existingVM = validationModels.getElement(applicablePath).getValue();
                        if (existingVM != null) {
                            String existingModelRelativeModelPath = existingVM.getJcrPath().replaceFirst(searchPath, "");
                            if (existingModelRelativeModelPath.equals(relativeModelPath)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
    
   
    
    private void validatePropertyValue(ValidationResultImpl result, String property, String relativePath, ValueMap valueMap, List<ParameterizedValidator> validators) {
        for (ParameterizedValidator validator : validators) {
            // convert the type always to an array
            Class<?> type = validator.getType();
            if (!type.isArray()) {
                try {
                    // https://docs.oracle.com/javase/6/docs/api/java/lang/Class.html#getName%28%29 has some hints on class names
                    type = Class.forName("[L"+type.getName()+";", false, type.getClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new SlingValidationException("Could not generate array class for type " + type, e);
                }
            }
            
            Object[] typedValue = (Object[])valueMap.get(property, type);
            // see https://issues.apache.org/jira/browse/SLING-4178 for why the second check is necessary
            if (typedValue == null || (typedValue.length > 0 && typedValue[0] == null)) {
                // here the missing required property case was already treated in validateValueMap
                result.addFailureMessage(property, "Property was expected to be of type '" + validator.getType() + "' but cannot be converted to that type." );
                return;
            }
            
            // see https://issues.apache.org/jira/browse/SLING-662 for a description on how multivalue properties are treated with ValueMap
            if (validator.getType().isArray()) {
                // ValueMap already returns an array in both cases (property is single value or multivalue)
                validateValue(result, typedValue, property, relativePath, valueMap, validator);
            } else {
                // call validate for each entry in the array (supports both singlevalue and multivalue)
                if (typedValue.getClass().isArray()) {
                    Object[] array = (Object[])typedValue;
                    if (array.length == 1) {
                        validateValue(result, array[0], property, relativePath, valueMap, validator);
                    } else {
                        int n = 0;
                        for (Object item : array) {
                            validateValue(result, item, property + "[" + n++ + "]", relativePath, valueMap, validator);
                        }
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("rawtypes")
    private void validateValue(ValidationResultImpl result, Object value, String property, String relativePath, ValueMap valueMap, ParameterizedValidator validator) {
        try {
            @SuppressWarnings("unchecked")
            String validatorMessage = ((Validator)validator.getValidator()).validate(value, valueMap, validator.getParameters());
            if (validatorMessage != null) {
                if (validatorMessage.isEmpty()) {
                    validatorMessage = "Property does not contain a valid value for the " + validator
                            .getClass().getName() + " validator";
                } 
                result.addFailureMessage(relativePath + property, validatorMessage);
            }
        } catch (SlingValidationException e) {
            // wrap in another SlingValidationException to include information about the property
            throw new SlingValidationException("Could not call validator " + validator
                    .getClass().getName() + " for resourceProperty " + relativePath + property, e);
        }
    }
    
    // OSGi ################################################################################################################################
    protected void bindValidator(Validator<?> validator, Map<?, ?> properties) {
        validators.put(validator.getClass().getName(), validator);
    }

    protected void unbindValidator(Validator<?> validator, Map<?, ?> properties) {
        // also remove references to all validators in the cache
        validationModelsCache.clear();
        validators.remove(validator.getClass().getName());
    }
}
