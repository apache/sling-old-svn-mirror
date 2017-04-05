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
package org.apache.sling.samples.fling.form;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;

public class BaseForm implements Form {

    private ValidationResult validationResult;

    protected final Map<String, Object> fields = new HashMap<>();

    public static final String RESOURCE_TYPE = "fling/form";

    public BaseForm() {
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public void setValidationResult(final ValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    @Override
    public ValidationResult getValidationResult() {
        return validationResult;
    }

    @Override
    public boolean hasFailure(final String name) {
        if (validationResult == null) {
            return false;
        }
        if (validationResult.isValid()) {
            return false;
        }
        for (final ValidationFailure failure : validationResult.getFailures()) {
            if (name.equals(failure.getLocation())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ValidationFailure> getFailures(final String name) {
        if (validationResult == null) {
            return Collections.emptyList();
        }
        if (validationResult.isValid()) {
            return Collections.emptyList();
        }
        return validationResult.getFailures()
            .stream()
            .filter(failure -> name.equals(failure.getLocation()))
            .collect(Collectors.toList());
    }

    @Override
    public int size() {
        return fields.size();
    }

    @Override
    public boolean isEmpty() {
        return fields.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return fields.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return fields.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return fields.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return fields.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return fields.remove(key);
    }

    @Override
    public void putAll(@Nonnull Map<? extends String, ?> m) {
        fields.putAll(m);
    }

    @Override
    public void clear() {
        fields.clear();
    }

    @Override
    @Nonnull
    public Set<String> keySet() {
        return fields.keySet();
    }

    @Override
    @Nonnull
    public Collection<Object> values() {
        return fields.values();
    }

    @Override
    @Nonnull
    public Set<Entry<String, Object>> entrySet() {
        return fields.entrySet();
    }

}
