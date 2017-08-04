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
package org.apache.sling.validation.impl.annotationmodel.testvalidators;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.spi.ValidatorContext;
import org.apache.sling.validation.spi.Validator;
import org.apache.sling.validation.spi.support.DefaultValidationResult;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(property = "validator.id=MinimumLengthValidator")
public class MinimumLengthValidator implements Validator<String> {

    private static final Logger LOG = LoggerFactory.getLogger(MinimumLengthValidator.class);

    private static final String MIN_LENGTH_FIELD = "minLength";

    @Nonnull
    @Override public ValidationResult validate(@Nonnull String s, @Nonnull ValidatorContext validationContext, @Nonnull ValueMap valueMap) {
        Resource r = validationContext.getResource();
        LOG.debug("validating minimum length of property [{}] in [{}]", validationContext.getLocation(), r != null ? r.getPath() : "null");
        LOG.debug("property [{}] equals [{}]", validationContext.getLocation(), s);
        int minLength = valueMap.get(MIN_LENGTH_FIELD, 0);
        LOG.debug("and should be longer than [{}] characters", minLength);
        if (minLength < 1) {
            LOG.error("{} was not properly configured, please add a positive integer \"{}\" property to its validation model!", this.getClass().getTypeName(), MIN_LENGTH_FIELD);
        }
        if (s.length() < minLength) {
            LOG.warn("[{}] is too short!", validationContext.getLocation());
            return new DefaultValidationResult(validationContext, "Property \"{0}\" must be at least {1} characters long.", validationContext.getLocation(), minLength);
        } else {
            return new DefaultValidationResult();
        }
    }
}
