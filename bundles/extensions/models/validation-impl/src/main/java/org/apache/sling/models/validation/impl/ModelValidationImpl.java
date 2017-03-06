/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.validation.impl;

import javax.annotation.Nonnull;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.InvalidModelException;
import org.apache.sling.models.factory.ValidationException;
import org.apache.sling.models.spi.ModelValidation;
import org.apache.sling.models.validation.InvalidResourceException;
import org.apache.sling.validation.SlingValidationException;
import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.ValidationService;
import org.apache.sling.validation.model.ValidationModel;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a validation service for Sling Models based on Sling Validation.
 * It enforces a validation of the resource which is adapted to the model.
 * @see <a href="http://sling.apache.org/documentation/bundles/validation.html">Sling Validation</a>
 */
@Component
@Designate(ocd=ModelValidationConfiguration.class)
public class ModelValidationImpl implements ModelValidation {

    @Reference
    private ValidationService validation;
    
    private ModelValidationConfiguration configuration;
    
    private static final Logger log = LoggerFactory.getLogger(ModelValidationImpl.class);
    
    @Activate
    protected void activate(ModelValidationConfiguration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * Triggers validation for the given model on the given adaptable.
     * @param adaptable {@inheritDoc}
     * @param modelClass {@inheritDoc}
     * @param required {@inheritDoc}
     * @return a {@link ValidationException} in case validation could not be performed if validation model could not be performed but 
     * required=true or if the adaptable is neither a Resource nor a SlingHttpServletRequest.
     * Or a {@link InvalidResourceException} in case the given resource (in the adaptable) could not be validated through the {@link ModelValidation}.
     */
    public <ModelType> RuntimeException validate(Object adaptable, Class<ModelType> modelClass, boolean required) throws ValidationException, InvalidModelException {
        if (configuration.disabled()) {
            log.debug("Skip validation of model {}, because  validation is disabled through the OSGi configuration for ModelValidationConfiguration", modelClass);
            return null;
        }
        Resource resource = null;
        if (adaptable instanceof SlingHttpServletRequest) {
            resource = ((SlingHttpServletRequest)adaptable).getResource();
        } else if (adaptable instanceof Resource) {
            resource = (Resource)adaptable;
        }
        if (resource != null) {
            return validate(resource, required);
        } else {
            return new ValidationException("Sling Validation can only be performed if model is adapted from either SlingHttpServletRequest or Resource.");
        }
    }
    
    private RuntimeException validate(@Nonnull Resource resource, boolean required) {
        try {
            ValidationModel validationModel = validation.getValidationModel(resource, true);
            if (validationModel == null) {
                String error = String.format("Could not find validation model for resource '%s' with type '%s'", resource.getPath(), resource.getResourceType());
                if (required) {
                    return new ValidationException(error);
                } else {
                    log.debug(error);
                }
            } else {
                try {
                    ValidationResult validationResult = validation.validate(resource, validationModel);
                    if (!validationResult.isValid()) {
                        boolean shouldThrow = false;
                        // evaluate all severities
                        for (ValidationFailure failure : validationResult.getFailures()) {
                            if (failure.getSeverity() >= configuration.severityThreshold()) {
                                shouldThrow = true;
                                break;
                            }
                        }
                        if (shouldThrow) {
                            return new InvalidResourceException("Sling Model is invalid", validationResult, resource.getPath());
                        } else {
                            log.debug("Although the resource {} is considered invalid by Sling Validation, all validation failures have a severity below the threshold '{}', "
                                    + "therefore considering this Sling Model valid.", resource.getPath(), configuration.severityThreshold());
                        }
                    }
                } catch (SlingValidationException e) {
                    return new ValidationException(e);
                }
            }
        } catch (IllegalStateException e) {
            return new ValidationException(e);
        }
        return null;
    }
}
