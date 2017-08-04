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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.validation.SlingValidationException;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.ValidationService;
import org.apache.sling.validation.impl.ValidatorMap.ValidatorMetadata;
import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ValidatorInvocation;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.model.spi.ValidationModelRetriever;
import org.apache.sling.validation.spi.ValidatorContext;
import org.apache.sling.validation.spi.Validator;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Designate(ocd=ValidationServiceConfiguration.class)
public class ValidationServiceImpl implements ValidationService{

    /** Keys whose values are defined in the JCR resource bundle contained in the content-repository section of this bundle */
    protected static final @Nonnull String I18N_KEY_WRONG_PROPERTY_TYPE = "sling.validator.wrong-property-type";
    protected static final @Nonnull String I18N_KEY_EXPECTED_MULTIVALUE_PROPERTY = "sling.validator.multi-value-property-required";
    protected static final @Nonnull String I18N_KEY_MISSING_REQUIRED_PROPERTY_WITH_NAME = "sling.validator.missing-required-property-with-name";
    protected static final @Nonnull String I18N_KEY_MISSING_REQUIRED_PROPERTY_MATCHING_PATTERN = "sling.validator.missing-required-property-matching-pattern";
    protected static final @Nonnull String I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_WITH_NAME = "sling.validator.missing-required-child-resource-with-name";
    protected static final @Nonnull String I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_MATCHING_PATTERN = "sling.validator.missing-required-child-resource-matching-pattern";

    private static final Logger LOG = LoggerFactory.getLogger(ValidationServiceImpl.class);
    
    @Reference
    ValidationModelRetriever modelRetriever;
    
    /** List of all known validators (key=id of validator) */
    @Nonnull
    final ValidatorMap validatorMap;
    
    Collection<String> searchPaths;
    
    ValidationServiceConfiguration configuration;
    
    @Reference
    private ResourceResolverFactory rrf = null;
    
    /** 
     * List of resource bundle providers, Declarative Services 1.3 takes care that the list is ordered according to {@link ServiceReference#compareTo(Object)}.
     * Highest ranked service is the last one in the list.
     * 
     * @see OSGi R6 Comp, 112.3.8.1
     */
    @Reference(policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.AT_LEAST_ONE, policyOption=ReferencePolicyOption.GREEDY)
    volatile List<ResourceBundleProvider> resourceBundleProviders;

    @Reference
    private ServiceUserMapped serviceUserMapped;
    
    public ValidationServiceImpl() {
        this.validatorMap = new ValidatorMap();
    }

    @Activate
    protected void activate(ValidationServiceConfiguration configuration) {
        this.configuration = configuration;
        ResourceResolver rr = null;
        try {
            rr = rrf.getServiceResourceResolver(null);
            searchPaths = Arrays.asList(rr.getSearchPath());
        } catch (LoginException e) {
            throw new IllegalStateException("Could not get service resource resolver to figure out search paths", e);
        } finally {
            if (rr != null) {
                rr.close();
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY, policy=ReferencePolicy.DYNAMIC)
    protected void addValidator(@Nonnull Validator<?> validator, Map<String, Object> properties, ServiceReference<Validator<?>> serviceReference) {
        validatorMap.put(properties, validator, serviceReference);
    }

    protected void removeValidator(@Nonnull Validator<?> validator, Map<String, Object> properties, ServiceReference<Validator<?>> serviceReference) {
        validatorMap.remove(properties, validator, serviceReference);
    }
    
    /** 
     * Necessary to deal with property changes which do not lead to service restarts (when a modified method is provided)
     */
    protected void updatedValidator(@Nonnull Validator<?> validator, Map<String, Object> properties, ServiceReference<Validator<?>> serviceReference) {
        validatorMap.update(properties, validator, serviceReference);
    }



    // ValidationService ###################################################################################################################
    
    public @CheckForNull ValidationModel getValidationModel(@Nonnull String validatedResourceType, String resourcePath, boolean considerResourceSuperTypeModels) {
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=459256
        if (validatedResourceType == null) {
            throw new IllegalArgumentException("ValidationService.getValidationModel - cannot accept null as resource type. Resource path was: " + resourcePath);
        }
        // convert to relative resource types, see https://issues.apache.org/jira/browse/SLING-4262
        validatedResourceType = getRelativeResourceType(validatedResourceType);
        return modelRetriever.getValidationModel(validatedResourceType, resourcePath,  considerResourceSuperTypeModels);
    }
    
    
    /**
     * If the given resourceType is starting with a "/", it will strip out the leading search path from the given resource type.
     * Otherwise it will just return the given resource type (as this is already relative).
     * @param resourceType the resource type to convert
     * @return a relative resource type (without the leading search path)
     * @throws IllegalArgumentException in case the resource type is starting with a "/" but not with any of the search paths.
     */
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

    @Override
    public @CheckForNull ValidationModel getValidationModel(@Nonnull Resource resource, boolean considerResourceSuperTypeModels) {
        return getValidationModel(resource.getResourceType(), resource.getPath(), considerResourceSuperTypeModels);
    }

    @Override
    public @Nonnull ValidationResult validate(@Nonnull Resource resource, @Nonnull ValidationModel model) {
        return validate(resource, model, "");
    }

    private @Nonnull ResourceBundle getDefaultResourceBundle() {
        Locale locale = Locale.ENGLISH;
        // go from highest ranked to lowest ranked providers
        for (int i = resourceBundleProviders.size() - 1; i >= 0; i--) {
            ResourceBundleProvider resourceBundleProvider = resourceBundleProviders.get(i);
            ResourceBundle defaultResourceBundle = resourceBundleProvider.getResourceBundle(locale);
            if (defaultResourceBundle != null) {
                return defaultResourceBundle;
            }
        }
        throw new IllegalStateException("There is no resource provider in the system, providing a resource bundle for locale");
    }

    private int getSeverityForValidator(Integer severityFromModel, Integer severityFromValidator) {
        if (severityFromModel != null) {
            return severityFromModel;
        }
        if (severityFromValidator != null) {
            return severityFromValidator;
        }
        return configuration.defaultSeverity();
    }

    protected @Nonnull ValidationResult validate(@Nonnull Resource resource, @Nonnull ValidationModel model, @Nonnull String relativePath) {
        if (resource == null || model == null || relativePath == null) {
            throw new IllegalArgumentException("ValidationService.validate - cannot accept null parameters");
        }
        ResourceBundle defaultResourceBundle = getDefaultResourceBundle();
        CompositeValidationResult result = new CompositeValidationResult();
        ValueMap valueMap = resource.adaptTo(ValueMap.class);
        if (valueMap == null) {
            // SyntheticResources can not adapt to a ValueMap, therefore just use the empty map here
            valueMap = new ValueMapDecorator(Collections.emptyMap());
        }

        // validate direct properties of the resource
        validateValueMap(valueMap, resource, relativePath, model.getResourceProperties(), result, defaultResourceBundle);

        // validate child resources, if any
        validateChildren(resource, relativePath, model.getChildren(), result, defaultResourceBundle);
        
        // optionally put result to cache
        if (configuration.cacheValidationResultsOnResources()) {
            ResourceToValidationResultAdapterFactory.putValidationResultToCache(result, resource);
        }
        return result;
    }

   

    /**
     * Validates a child resource with the help of the given {@code ChildResource} entry from the validation model
     * @param resource
     * @param relativePath relativePath of the resource (must be empty or end with "/")
     * @param result
     * @param childResources
     */
    private void validateChildren(@Nonnull Resource resource, @Nonnull String relativePath, @Nonnull Collection<ChildResource> childResources, @Nonnull CompositeValidationResult result, @Nonnull ResourceBundle defaultResourceBundle) {
        // validate children resources, if any
        for (ChildResource childResource : childResources) {
            // if a pattern is set we validate all children matching that pattern
            Pattern pattern = childResource.getNamePattern();
            if (pattern != null) {
                boolean foundMatch = false;
                for (Resource child : resource.getChildren()) {
                    Matcher matcher = pattern.matcher(child.getName());
                    if (matcher.matches()) {
                       validateChildResource(child, relativePath, childResource, result, defaultResourceBundle);
                       foundMatch = true;
                    }
                }
                if (!foundMatch && childResource.isRequired()) {
                    result.addFailure(relativePath, configuration.defaultSeverity(), defaultResourceBundle, I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_MATCHING_PATTERN, pattern.toString());
                }
            } else {
                Resource expectedResource = resource.getChild(childResource.getName());
                if (expectedResource != null) {
                    validateChildResource(expectedResource, relativePath, childResource, result, defaultResourceBundle);
                } else if (childResource.isRequired()) {
                    result.addFailure(relativePath, configuration.defaultSeverity(), defaultResourceBundle, I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_WITH_NAME, childResource.getName());
                }
            } 
        }
    }

    private void validateChildResource(@Nonnull Resource resource, @Nonnull String relativePathOfParent, @Nonnull ChildResource childResource, @Nonnull CompositeValidationResult result, @Nonnull ResourceBundle defaultResourceBundle) {
        final @Nonnull String relativePath;
        if (relativePathOfParent.isEmpty()) {
            relativePath = resource.getName();
        } else {
            relativePath = relativePathOfParent +  "/" + resource.getName();
        }
        validateValueMap(resource.adaptTo(ValueMap.class), resource, relativePath, childResource.getProperties(), result, defaultResourceBundle);
        validateChildren(resource, relativePath, childResource.getChildren(), result, defaultResourceBundle);
    }

    @Override
    public @Nonnull ValidationResult validate(@Nonnull ValueMap valueMap, @Nonnull ValidationModel model) {
        if (valueMap == null || model == null) {
            throw new IllegalArgumentException("ValidationResult.validate - cannot accept null parameters");
        }
        ResourceBundle defaultResourceBundle = getDefaultResourceBundle();
        CompositeValidationResult result = new CompositeValidationResult();
        validateValueMap(valueMap, null, "", model.getResourceProperties(), result, defaultResourceBundle);
        return result;
    }    

    @Override
    public @Nonnull ValidationResult validateResourceRecursively(@Nonnull Resource resource, boolean enforceValidation, Predicate<Resource> filter, boolean considerResourceSuperTypeModels)
            throws IllegalStateException, IllegalArgumentException, SlingValidationException {
        ValidationResourceVisitor visitor = new ValidationResourceVisitor(this, resource.getPath(), enforceValidation, filter, considerResourceSuperTypeModels);
        visitor.accept(resource);
        return visitor.getResult();
    }

    private void validateValueMap(ValueMap valueMap, Resource resource, @Nonnull String relativePath, @Nonnull Collection<ResourceProperty> resourceProperties,
            @Nonnull CompositeValidationResult result, @Nonnull ResourceBundle defaultResourceBundle) {
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
                        validatePropertyValue(key, valueMap, resource, relativePath, resourceProperty, result, defaultResourceBundle);
                    }
                }
                if (!foundMatch && resourceProperty.isRequired()) {
                    result.addFailure(relativePath, configuration.defaultSeverity(), defaultResourceBundle, I18N_KEY_MISSING_REQUIRED_PROPERTY_MATCHING_PATTERN, pattern.toString());
                }
            } else {
                validatePropertyValue(resourceProperty.getName(), valueMap, resource, relativePath, resourceProperty, result, defaultResourceBundle);
            }
        }
    }

    private void validatePropertyValue(@Nonnull String property, ValueMap valueMap, Resource resource, @Nonnull String relativePath, @Nonnull ResourceProperty resourceProperty, @Nonnull CompositeValidationResult result, @Nonnull ResourceBundle defaultResourceBundle) {
        Object fieldValues = valueMap.get(property);
        if (fieldValues == null) {
            if (resourceProperty.isRequired()) {
                result.addFailure(relativePath, configuration.defaultSeverity(), defaultResourceBundle, I18N_KEY_MISSING_REQUIRED_PROPERTY_WITH_NAME, property);
            }
            return;
        }
        List<ValidatorInvocation> validatorInvocations = resourceProperty.getValidatorInvocations();
        if (resourceProperty.isMultiple()) {
            if (!fieldValues.getClass().isArray()) {
                result.addFailure(relativePath + property, configuration.defaultSeverity(), defaultResourceBundle, I18N_KEY_EXPECTED_MULTIVALUE_PROPERTY);
                return;
            }
        }
        
        for (ValidatorInvocation validatorInvocation : validatorInvocations) {
            // lookup validator by id
            ValidatorMetadata validatorMetadata = validatorMap.get(validatorInvocation.getValidatorId());
            if (validatorMetadata == null) {
                throw new IllegalStateException("Could not find validator with id '" + validatorInvocation.getValidatorId() + "'");
            }
            int severity = getSeverityForValidator(validatorInvocation.getSeverity(), validatorMetadata.getSeverity());
            
            // convert the type always to an array
            Class<?> type = validatorMetadata.getType();
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
                result.addFailure(relativePath + property, severity, defaultResourceBundle, I18N_KEY_WRONG_PROPERTY_TYPE, validatorMetadata.getType());
                return;
            }
            
            // see https://issues.apache.org/jira/browse/SLING-662 for a description on how multivalue properties are treated with ValueMap
            if (validatorMetadata.getType().isArray()) {
                // ValueMap already returns an array in both cases (property is single value or multivalue)
                validateValue(result, typedValue, property, relativePath, valueMap, resource, validatorMetadata.getValidator(), validatorInvocation.getParameters(), defaultResourceBundle, severity);
            } else {
                // call validate for each entry in the array (supports both singlevalue and multivalue)
                @Nonnull Object[] array = (Object[])typedValue;
                if (array.length == 1) {
                   validateValue(result, array[0], property, relativePath, valueMap, resource, validatorMetadata.getValidator(), validatorInvocation.getParameters(), defaultResourceBundle, severity);
                } else {
                    int n = 0;
                    for (Object item : array) {
                        validateValue(result, item, property + "[" + n++ + "]", relativePath, valueMap, resource, validatorMetadata.getValidator(), validatorInvocation.getParameters(), defaultResourceBundle, severity);
                    }
                }
            }
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void validateValue(CompositeValidationResult result, @Nonnull Object value, String property, String relativePath, @Nonnull ValueMap valueMap, Resource resource, @Nonnull Validator validator, ValueMap validatorParameters, @Nonnull ResourceBundle defaultResourceBundle, int severity) {
        try {
            ValidatorContext validationContext = new ValidatorContextImpl(relativePath + property, severity, valueMap, resource, defaultResourceBundle);
            ValidationResult validatorResult = ((Validator)validator).validate(value, validationContext, validatorParameters);
            result.addValidationResult(validatorResult);
        } catch (SlingValidationException e) {
            // wrap in another SlingValidationException to include information about the property
            throw new SlingValidationException("Could not call validator " + validator
                    .getClass().getName() + " for resourceProperty " + relativePath + property, e);
        }
    }
    
}
