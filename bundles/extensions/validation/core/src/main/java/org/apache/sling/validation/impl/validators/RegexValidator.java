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
package org.apache.sling.validation.impl.validators;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.SlingValidationException;
import org.apache.sling.validation.spi.DefaultValidationResult;
import org.apache.sling.validation.spi.ValidationContext;
import org.apache.sling.validation.spi.Validator;

/**
 * Performs regular expressions validation on the supplied data with the help of the {@link Pattern} class. This {@code Validator} expects a
 * mandatory parameter in the arguments map: {@link RegexValidator#REGEX_PARAM}.
 */
@Component()
@Service(Validator.class)
public class RegexValidator implements Validator<String> {

    public static final String I18N_KEY_PATTERN_DOES_NOT_MATCH = "sling.validator.regex.pattern-does-not-match";
    public static final String REGEX_PARAM = "regex";

    @Override
    public @Nonnull ValidationResult validate(@Nonnull String data, @Nonnull ValidationContext context, @Nonnull ValueMap arguments)
            throws SlingValidationException {
        String regex = arguments.get(REGEX_PARAM, "");
        if (StringUtils.isEmpty(regex)) {
            throw new SlingValidationException("Mandatory argument '" + REGEX_PARAM + "' is missing from the arguments map.");
        }
        try {
            Pattern pattern = Pattern.compile(regex);
            if (pattern.matcher((String)data).matches()) {
                return DefaultValidationResult.VALID;
            }
            return new DefaultValidationResult(context.getLocation(), context.getSeverity(), I18N_KEY_PATTERN_DOES_NOT_MATCH, regex);
        } catch (PatternSyntaxException e) {
            throw new SlingValidationException("Given pattern in argument '" + REGEX_PARAM + "' is invalid", e);
        }
    }

}
