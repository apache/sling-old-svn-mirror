/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.examples.models;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.ValidationService;
import org.apache.sling.validation.model.ValidationModel;

@Model(adaptables = Resource.class)
public class UserModel {

    private static final String INVALID = "INVALID";

    private Resource resource;
    private List<ValidationFailure> errors = new LinkedList<ValidationFailure>();

    @Inject
    private ValidationService validationService;

    @Inject
    private String username;

    @Inject
    private String firstName;

    @Inject
    private String lastName;

    @Inject
    private boolean isAdmin;

    public UserModel(Resource resource) {
        this.resource = resource;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public List<ValidationFailure> getErrors() {
        return errors;
    }

    @PostConstruct
    protected void validate() {
        ValidationModel model = validationService.getValidationModel(resource, false);
        if (model != null) {
            ValidationResult result = validationService.validate(resource, model);
            if (!result.isValid()) {
                errors = result.getFailures();
            }
        }
    }
}
