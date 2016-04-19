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
package org.apache.sling.samples.fling.validation;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.SlingValidationException;
import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.spi.DefaultValidationFailure;
import org.apache.sling.validation.spi.DefaultValidationResult;
import org.apache.sling.validation.spi.ValidationContext;
import org.apache.sling.validation.spi.Validator;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

@Component(
    service = Validator.class,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Fling Sample “Comment Validator”",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    },
    immediate = true
)
public class CommentValidator implements Validator<String> {

    private static final String COMMENT_PARAMETER = "comment";

    private static final String I18N_MESSAGE_KEY = "fling.validator.comment.invalid";

    @Override
    @Nonnull
    public ValidationResult validate(@Nonnull String data, @Nonnull ValidationContext validationContext, @Nonnull ValueMap arguments) throws SlingValidationException {
        final String comment = arguments.get(COMMENT_PARAMETER, String.class);
        if (comment == null) {
            throw new SlingValidationException("Valid comment is missing.");
        }
        if (comment.equals(data)) {
            return DefaultValidationResult.VALID;

        } else {
            final ValidationFailure failure = new DefaultValidationFailure(validationContext.getLocation(), 0, I18N_MESSAGE_KEY, comment);
            return new DefaultValidationResult(failure);
        }
    }

}
