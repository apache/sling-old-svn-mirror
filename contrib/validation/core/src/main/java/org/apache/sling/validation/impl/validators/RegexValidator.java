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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.api.exceptions.SlingValidationException;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Performs regular expressions validation on the supplied data with the help of the {@link Pattern} class. This {@code Validator} expects a
 * mandatory parameter in the arguments map: {@link RegexValidator#REGEX_PARAM}.
 */
@Component()
@Service(Validator.class)
public class RegexValidator implements Validator {

    public static final String REGEX_PARAM = "regex";

    @Override
    public boolean validate(String data, Map<String, String> arguments) {
        if (data == null || arguments == null) {
            throw new SlingValidationException("Cannot perform data validation with null parameters");
        }
        String regex = arguments.get(REGEX_PARAM);
        if (regex == null) {
            throw new SlingValidationException("Mandatory " + REGEX_PARAM + " is missing from the arguments map.");
        }
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(data).matches();
    }
}
