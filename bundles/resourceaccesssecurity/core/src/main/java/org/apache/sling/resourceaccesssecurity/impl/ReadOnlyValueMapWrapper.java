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

package org.apache.sling.resourceaccesssecurity.impl;

import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 *  Wrapper class that does protect the underlying map from modifications.
 */
public class ReadOnlyValueMapWrapper extends ValueMapDecorator
        implements ValueMap {

    /**
     * Creates a new wrapper around a given map.
     *
     * @param base wrapped object
     */
    public ReadOnlyValueMapWrapper(Map<String, Object> base) {
        super(base);
    }

    @Override
    public Object put(String key, Object value) {
        // TODO we probably should log this as a warning
        return null;
    }

    @Override
    public Object remove(Object key) {
        // TODO we probably should log this as a warning
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ?> t) {
        // TODO we probably should log this as a warning
    }

    @Override
    public void clear() {
        // TODO we probably should log this as a warning
    }
}
