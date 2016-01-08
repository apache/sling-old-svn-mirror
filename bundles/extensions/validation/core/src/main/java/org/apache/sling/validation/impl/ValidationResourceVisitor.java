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

import javax.annotation.Nonnull;

import org.apache.commons.collections.Predicate;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.model.ValidationModel;

public class ValidationResourceVisitor extends AbstractResourceVisitor {

    private final ValidationServiceImpl validationService;
    private final String rootResourcePath;
    private final boolean enforceValidation;
    private final boolean considerResourceSuperTypeModels;
    private final @Nonnull CompositeValidationResult result;
    private final Predicate filter;

    public ValidationResourceVisitor(ValidationServiceImpl validationService, String rootResourcePath, boolean enforceValidation, Predicate filter,  boolean considerResourceSuperTypeModels) {
        super();
        this.validationService = validationService;
        this.rootResourcePath = rootResourcePath + "/";
        this.enforceValidation = enforceValidation;
        this.considerResourceSuperTypeModels = considerResourceSuperTypeModels;
        this.filter = filter;
        this.result = new CompositeValidationResult();
    }

    @Override
    protected void visit(Resource resource) {
        if (isValidSubResource(resource)) {
            @SuppressWarnings("null")
            ValidationModel model = validationService.getValidationModel(resource, considerResourceSuperTypeModels);
            if (model == null) {
                if (enforceValidation) {
                    throw new IllegalArgumentException("No model for resource type " + resource.getResourceType() + " found.");
                }
                return;
            }
            // calculate the property name correctly from the root
            // the relative path must not end with a slash and not start with a slash
            final String relativePath;
            if (resource.getPath().startsWith(rootResourcePath)) {
                relativePath = resource.getPath().substring(rootResourcePath.length());
            } else {
                relativePath = "";
            }
            ValidationResult localResult = validationService.validate(resource, model, relativePath);
            result.addValidationResult(localResult);
        }
    }
    
    /**
     * 
     * @return {@code true} in case the given resource should have its own Sling Validation model
     */
    private boolean isValidSubResource(Resource resource) {
        if (ResourceUtil.isNonExistingResource(resource)) {
            return false;
        }
        if (filter != null) {
            return filter.evaluate(resource);
        }
        return true;
    }

    public @Nonnull CompositeValidationResult getResult() {
        return result;
    }

}
