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

import org.apache.felix.scr.annotations.Component;
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
import org.apache.sling.validation.api.ChildResource;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.Type;
import org.apache.sling.validation.api.ValidationModel;
import org.apache.sling.validation.api.ValidationResult;
import org.apache.sling.validation.api.ValidationService;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.api.ValidatorLookupService;
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

import javax.jcr.query.Query;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component()
@Service(ValidationService.class)
public class ValidationServiceImpl implements ValidationService, EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationServiceImpl.class);

    static final String MODEL_XPATH_QUERY = "/jcr:root/%s/" + Constants.MODELS_HOME + "*[@sling:resourceType=\"%s\" and @%s=\"%s\"]";
    static final String[] TOPICS = {SlingConstants.TOPIC_RESOURCE_REMOVED, SlingConstants.TOPIC_RESOURCE_CHANGED,
            SlingConstants.TOPIC_RESOURCE_ADDED};

    private Map<String, Trie<JCRValidationModel>> validationModelsCache = new ConcurrentHashMap<String, Trie<JCRValidationModel>>();
    private ThreadPool threadPool;
    private ServiceRegistration eventHandlerRegistration;

    @Reference
    private ResourceResolverFactory rrf = null;

    @Reference
    private ValidatorLookupService validatorLookupService = null;

    @Reference
    private ThreadPoolManager tpm = null;

    // ValidationService ###################################################################################################################
    @Override
    public ValidationModel getValidationModel(String validatedResourceType, String resourcePath) {
        ValidationModel model = null;
        Trie<JCRValidationModel> modelsForResourceType = validationModelsCache.get(validatedResourceType);
        if (modelsForResourceType != null) {
            model = modelsForResourceType.getElementForLongestMatchingKey(resourcePath).getValue();
        }
        if (model == null) {
            modelsForResourceType = searchAndStoreValidationModel(validatedResourceType);
            if (modelsForResourceType != null) {
                model = modelsForResourceType.getElementForLongestMatchingKey(resourcePath).getValue();
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
        if (resource == null || model == null) {
            throw new IllegalArgumentException("ValidationResult.validate - cannot accept null parameters");
        }
        ValidationResultImpl result = new ValidationResultImpl();

        // validate direct properties of the resource
        validateResourceProperties(resource, resource, model.getResourceProperties(), result);

        // validate children resources, if any
        for (ChildResource childResource : model.getChildren()) {
            Resource expectedResource = resource.getChild(childResource.getName());
            if (expectedResource != null) {
                validateResourceProperties(resource, expectedResource, childResource.getProperties(), result);
            } else {
                result.addFailureMessage(childResource.getName(), "Missing required child resource.");
            }
        }
        return result;
    }

    @Override
    public ValidationResult validate(ValueMap valueMap, ValidationModel model) {
        if (valueMap == null || model == null) {
            throw new IllegalArgumentException("ValidationResult.validate - cannot accept null parameters");
        }
        ValidationResultImpl result = new ValidationResultImpl();
        for (ResourceProperty resourceProperty : model.getResourceProperties()) {
            String property = resourceProperty.getName();
            Object valuesObject = valueMap.get(property);
            if (valuesObject == null) {
                result.addFailureMessage(property, "Missing required property.");
            }
            Type propertyType = resourceProperty.getType();
            Map<Validator, Map<String, String>> validators = resourceProperty.getValidators();
            if (resourceProperty.isMultiple()) {
                if (valuesObject instanceof String[]) {
                    for (String fieldValue : (String[]) valuesObject) {
                        validatePropertyValue(result, property, fieldValue, propertyType, validators);
                    }
                } else {
                    result.addFailureMessage(property, "Expected multiple-valued property.");
                }
            } else {
                if (valuesObject instanceof String[]) {
                    // treat request attributes which are arrays
                    String[] fieldValues = (String[]) valuesObject;
                    if (fieldValues.length == 1) {
                        validatePropertyValue(result, property, fieldValues[0], propertyType, validators);
                    } else {
                        result.addFailureMessage(property, "Expected single-valued property.");
                    }
                } else if (valuesObject instanceof String) {
                    validatePropertyValue(result, property, (String) valuesObject, propertyType, validators);
                }
            }
        }
        return result;
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
                String path = searchPath + "/" + Constants.MODELS_HOME;
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

    private void validateResourceProperties(Resource rootResource, Resource resource, Set<ResourceProperty> resourceProperties,
                                            ValidationResultImpl result) {
        for (ResourceProperty resourceProperty : resourceProperties) {
            String property = resourceProperty.getName();
            ValueMap valueMap = resource.adaptTo(ValueMap.class);
            Object fieldValues = valueMap.get(property);
            String relativePath = resource.getPath().replace(rootResource.getPath(), "");
            if (relativePath.length() > 0) {
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
                property = relativePath + "/" + property;
            }
            if (fieldValues == null) {
                result.addFailureMessage(property, "Missing required property.");
            }
            Type propertyType = resourceProperty.getType();
            Map<Validator, Map<String, String>> validators = resourceProperty.getValidators();
            if (fieldValues instanceof String[]) {
                for (String fieldValue : (String[]) fieldValues) {
                    validatePropertyValue(result, property, fieldValue, propertyType, validators);
                }
            } else if (fieldValues instanceof String) {
                validatePropertyValue(result, property, (String) fieldValues, propertyType, validators);
            }
        }
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
        try {
            rr = rrf.getAdministrativeResourceResolver(null);
            String[] searchPaths = rr.getSearchPath();
            for (String searchPath : searchPaths) {
                if (searchPath.endsWith("/")) {
                    searchPath = searchPath.substring(0, searchPath.length() - 1);
                }
                final String queryString = String.format(MODEL_XPATH_QUERY, searchPath, Constants.VALIDATION_MODEL_RESOURCE_TYPE,
                        Constants.VALIDATED_RESOURCE_TYPE, validatedResourceType);
                Iterator<Resource> models = rr.findResources(queryString, Query.XPATH);
                while (models.hasNext()) {
                    Resource model = models.next();
                    LOG.info("Found validation model resource {}.", model.getPath());
                    String jcrPath = model.getPath();
                    ValueMap validationModelProperties = model.adaptTo(ValueMap.class);
                    String[] applicablePaths = PropertiesUtil.toStringArray(validationModelProperties.get(Constants.APPLICABLE_PATHS,
                            String[].class));
                    if (validatedResourceType != null && !"".equals(validatedResourceType)) {
                        Resource r = model.getChild(Constants.PROPERTIES);
                        if (r != null) {
                            Set<ResourceProperty> resourceProperties = JCRBuilder.buildProperties(validatorLookupService, r);
                            if (!resourceProperties.isEmpty()) {
                                List<ChildResource> children = JCRBuilder.buildChildren(model, model, validatorLookupService);
                                vm = new JCRValidationModel(jcrPath, resourceProperties, validatedResourceType, applicablePaths, children);
                                modelsForResourceType = validationModelsCache.get(validatedResourceType);
                                /**
                                 * if the modelsForResourceType is null the canAcceptModel will return true: performance optimisation so that
                                 * the Trie is created only if the model is accepted
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
                        }
                    }
                }
            }
        } catch (LoginException e) {
            LOG.error("Unable to obtain a resource resolver.", e);
        }
        if (rr != null) {
            rr.close();
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

    private void validatePropertyValue(ValidationResultImpl result, String property, String value, Type propertyType, Map<Validator,
            Map<String, String>> validators) {
        if (!propertyType.isValid(value)) {
            result.addFailureMessage(property, "Property was expected to be of type " + propertyType.getName());
        }
        for (Map.Entry<Validator, Map<String, String>> validatorEntry : validators.entrySet()) {
            Validator validator = validatorEntry.getKey();
            Map<String, String> arguments = validatorEntry.getValue();
            try {
                if (!validator.validate(value, arguments)) {
                    result.addFailureMessage(property, "Property does not contain a valid value for the " + validator
                            .getClass().getName() + " validator");
                }
            } catch (SlingValidationException e) {
                LOG.error("SlingValidationException for resourceProperty " + property, e);
                result.addFailureMessage(property, "Validator " + validator.getClass() + "encountered a problem: " + e.getMessage());
            }
        }
    }
}