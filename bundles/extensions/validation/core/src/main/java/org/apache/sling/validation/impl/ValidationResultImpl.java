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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.validation.ValidationResult;

public class ValidationResultImpl implements ValidationResult {

    private final @Nonnull Map<String, List<String>> failureMessages;
    private boolean isValid;

    public ValidationResultImpl() {
        isValid = true;
        failureMessages = new HashMap<String, List<String>>();
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @SuppressWarnings("null")
    @Override
    public @Nonnull Map<String, List<String>> getFailureMessages() {
        return Collections.unmodifiableMap(failureMessages);
    }

    public void addFailureMessage(String property, String failureMessage) {
        List<String> propertyMessages = failureMessages.get(property);
        if (propertyMessages == null) {
            propertyMessages = new ArrayList<String>();
            failureMessages.put(property, propertyMessages);
        }
        propertyMessages.add(failureMessage);
        isValid = false;
    }
}
