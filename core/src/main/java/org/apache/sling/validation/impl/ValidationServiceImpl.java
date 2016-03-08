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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.ValidationService;
import org.apache.sling.validation.SlingValidationException;
import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ParameterizedValidator;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.spi.ValidationContext;
import org.apache.sling.validation.spi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component()
@Service()
public class ValidationServiceImpl implements ValidationService{

    /** Keys whose values are defined in the JCR resource bundle contained in the content-repository section of this bundle */
    protected static final String I18N_KEY_WRONG_PROPERTY_TYPE = "sling.validator.wrong-property-type";
    protected static final String I18N_KEY_EXPECTED_MULTIVALUE_PROPERTY = "sling.validator.multi-value-property-required";
    protected static final String I18N_KEY_MISSING_REQUIRED_PROPERTY_WITH_NAME = "sling.validator.missing-required-property-with-name";
    protected static final String I18N_KEY_MISSING_REQUIRED_PROPERTY_MATCHING_PATTERN = "sling.validator.missing-required-property-matching-pattern";
    protected static final String I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_WITH_NAME = "sling.validator.missing-required-child-resource-with-name";
    protected static final String I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_MATCHING_PATTERN = "sling.validator.missing-required-child-resource-matching-pattern";
    

    private static final Logger LOG = LoggerFactory.getLogger(ValidationServiceImpl.class);
    
    @Reference
    ValidationModelRetriever modelRetriever;
    
    Collection<String> searchPaths;
    
    @Reference
    private ResourceResolverFactory rrf = null;
    
    
    @Activate
    public void activate() {
        ResourceResolver rr = null;
        try {
            rr = rrf.getAdministrativeResourceResolver(null);
            searchPaths = Arrays.asList(rr.getSearchPath());
        } catch (LoginException e) {
            throw new IllegalStateException("Could not login as administrator to figure out search paths", e);
        } finally {
            if (rr != null) {
                rr.close();
            }
        }
    }
    

    // ValidationService ###################################################################################################################
    
    @SuppressWarnings("unused")
    public @CheckForNull ValidationModel getValidationModel(@Nonnull String validatedResourceType, String resourcePath, boolean considerResourceSuperTypeModels) {
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=459256
        if (validatedResourceType == null) {
            throw new IllegalArgumentException("ValidationService.getValidationModel - cannot accept null as resource type. Resource path was: " + resourcePath);
        }
        // convert to relative resource types, see https://issues.apache.org/jira/browse/SLING-4262
        validatedResourceType = getRelativeResourceType(validatedResourceType);
        return modelRetriever.getModel(validatedResourceType, resourcePath,  considerResourceSuperTypeModels);
    }
    
    
    /**
     * If the given resourceType is starting with a "/", it will strip out the leading search path from the given resource type.
     * Otherwise it will just return the given resource type (as this is already relative).
     * @param resourceType
     * @return a relative resource type (without the leading search path)
     * @throws IllegalArgumentException in case the resource type is starting with a "/" but not with any of the search paths.
     */
    @SuppressWarnings("null")
    protected @Nonnull String getRelativeResourceType(@Nonnull String resourceType) throws IllegalArgumentException {
        if (resourceType.startsWith("/")) {
            LOG.debug("try to strip the search path from the resource type");
            for (String searchPath : searchPaths) {
                if (resourceType.startsWith(searchPath)) {
                    resourceType = resourceType.substring(searchPath.length());
                    return resourceType;
                }
            }
            throw new IllegalArgumentException(
                    "Can only deal with resource types inside the resource resolver's search path ("
                            + StringUtils.join(searchPaths.toArray()) + ") but given resource type " + resourceType
                            + " is outside!");
        }
        return resourceType;
    }

    @SuppressWarnings("null")
    @Override
    public @CheckForNull ValidationModel getValidationModel(@Nonnull Resource resource, boolean considerResourceSuperTypeModels) {
        return getValidationModel(resource.getResourceType(), resource.getPath(), considerResourceSuperTypeModels);
    }

    @Override
    public @Nonnull ValidationResult validate(@Nonnull Resource resource, @Nonnull ValidationModel model) {
        return validate(resource, model, "");
    }
    
    protected @Nonnull ValidationResult validate(@Nonnull Resource resource, @Nonnull ValidationModel model, @Nonnull String relativePath) {
        if (resource == null || model == null || relativePath == null) {
            throw new IllegalArgumentException("ValidationService.validate - cannot accept null parameters");
        }
        CompositeValidationResult result = new CompositeValidationResult();

        // validate direct properties of the resource
        validateValueMap(resource.adaptTo(ValueMap.class), resource, relativePath, model.getResourceProperties(), result );

        // validate children resources, if any
        validateChildren(resource, relativePath, model.getChildren(), result);
        return result;
    }

   

    /**
     * Validates a child resource with the help of the given {@code ChildResource} entry from the validation model
     * @param resource
     * @param relativePath relativePath of the resource (must be empty or end with "/")
     * @param result
     * @param childResources
     */
    private void validateChildren(Resource resource, String relativePath, Collection<ChildResource> childResources, CompositeValidationResult result) {
        // validate children resources, if any
        for (ChildResource childResource : childResources) {
            // if a pattern is set we validate all children matching that pattern
            Pattern pattern = childResource.getNamePattern();
            if (pattern != null) {
                boolean foundMatch = false;
                for (Resource child : resource.getChildren()) {
                    Matcher matcher = pattern.matcher(child.getName());
                    if (matcher.matches()) {
                       validateChildResource(child, relativePath, childResource, result);
                       foundMatch = true;
                    }
                }
                if (!foundMatch && childResource.isRequired()) {
                    result.addFailure(relativePath, I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_MATCHING_PATTERN, pattern.toString());
                }
            } else {
                Resource expectedResource = resource.getChild(childResource.getName());
                if (expectedResource != null) {
                    validateChildResource(expectedResource, relativePath, childResource, result);
                } else if (childResource.isRequired()) {
                    result.addFailure(relativePath, I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_WITH_NAME, childResource.getName());
                }
            } 
        }
    }

    private void validateChildResource(Resource resource, String relativePathOfParent, ChildResource childResource, CompositeValidationResult result) {
        final String relativePath;
        if (relativePathOfParent.isEmpty()) {
            relativePath = resource.getName();
        } else {
            relativePath = relativePathOfParent +  "/" + resource.getName();
        }
        validateValueMap(resource.adaptTo(ValueMap.class), resource, relativePath, childResource.getProperties(), result);
        validateChildren(resource, relativePath, childResource.getChildren(), result);
    }

    @Override
    public @Nonnull ValidationResult validate(@Nonnull ValueMap valueMap, @Nonnull ValidationModel model) {
        if (valueMap == null || model == null) {
            throw new IllegalArgumentException("ValidationResult.validate - cannot accept null parameters");
        }
        CompositeValidationResult result = new CompositeValidationResult();
        validateValueMap(valueMap, null, "", model.getResourceProperties(), result);
        return result;
    }    

    @Override
    public @Nonnull ValidationResult validateResourceRecursively(@Nonnull Resource resource, boolean enforceValidation, Predicate filter, boolean considerResourceSuperTypeModels)
            throws IllegalStateException, IllegalArgumentException, SlingValidationException {
        ValidationResourceVisitor visitor = new ValidationResourceVisitor(this, resource.getPath(), enforceValidation, filter, considerResourceSuperTypeModels);
        visitor.accept(resource);
        return visitor.getResult();
    }

    private void validateValueMap(ValueMap valueMap, Resource resource, String relativePath, Collection<ResourceProperty> resourceProperties,
            CompositeValidationResult result) {
        if (valueMap == null) {
            throw new IllegalArgumentException("ValueMap may not be null");
        }
        for (ResourceProperty resourceProperty : resourceProperties) {
            Pattern pattern = resourceProperty.getNamePattern();
            if (pattern != null) {
                boolean foundMatch = false;
                for (String key : valueMap.keySet()) {
                    if (pattern.matcher(key).matches()) {
                        foundMatch = true;
                        validatePropertyValue(key, valueMap, resource, relativePath, resourceProperty, result);
                    }
                }
                if (!foundMatch && resourceProperty.isRequired()) {
                    result.addFailure(relativePath, I18N_KEY_MISSING_REQUIRED_PROPERTY_MATCHING_PATTERN, pattern.toString());
                }
            } else {
                validatePropertyValue(resourceProperty.getName(), valueMap, resource, relativePath, resourceProperty, result);
            }
        }
    }

    private void validatePropertyValue(String property, ValueMap valueMap, Resource resource, String relativePath, ResourceProperty resourceProperty, CompositeValidationResult result) {
        Object fieldValues = valueMap.get(property);
        if (fieldValues == null) {
            if (resourceProperty.isRequired()) {
                result.addFailure(relativePath, I18N_KEY_MISSING_REQUIRED_PROPERTY_WITH_NAME, property);
            }
            return;
        }
        List<ParameterizedValidator> validators = resourceProperty.getValidators();
        if (resourceProperty.isMultiple()) {
            if (!fieldValues.getClass().isArray()) {
                result.addFailure(relativePath + property, I18N_KEY_EXPECTED_MULTIVALUE_PROPERTY);
                return;
            }
        }
        
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
            // it is already validated here that the property exists in the value map
            Object[] typedValue = (Object[])valueMap.get(property, type);
            // see https://issues.apache.org/jira/browse/SLING-4178 for why the second check is necessary
            if (typedValue == null || (typedValue.length > 0 && typedValue[0] == null)) {
                // here the missing required property case was already treated in validateValueMap
                result.addFailure(relativePath + property, I18N_KEY_WRONG_PROPERTY_TYPE, validator.getType());
                return;
            }
            
            // see https://issues.apache.org/jira/browse/SLING-662 for a description on how multivalue properties are treated with ValueMap
            if (validator.getType().isArray()) {
                // ValueMap already returns an array in both cases (property is single value or multivalue)
                validateValue(result, typedValue, property, relativePath, valueMap, resource, validator);
            } else {
                // call validate for each entry in the array (supports both singlevalue and multivalue)
                @Nonnull Object[] array = (Object[])typedValue;
                if (array.length == 1) {
                   validateValue(result, array[0], property, relativePath, valueMap, resource, validator);
                } else {
                    int n = 0;
                    for (Object item : array) {
                        validateValue(result, item, property + "[" + n++ + "]", relativePath, valueMap, resource, validator);
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("rawtypes")
    private void validateValue(CompositeValidationResult result, @Nonnull Object value, String property, String relativePath, @Nonnull ValueMap valueMap, Resource resource, ParameterizedValidator validator) {
        try {
            @SuppressWarnings("unchecked")
            ValidationContext validationContext = new ValidationContextImpl(relativePath + property, valueMap, resource);
            ValidationResult validatorResult = ((Validator)validator.getValidator()).validate(value, validationContext, validator.getParameters());
            result.addValidationResult(validatorResult);
        } catch (SlingValidationException e) {
            // wrap in another SlingValidationException to include information about the property
            throw new SlingValidationException("Could not call validator " + validator
                    .getClass().getName() + " for resourceProperty " + relativePath + property, e);
        }
    }
    
}
