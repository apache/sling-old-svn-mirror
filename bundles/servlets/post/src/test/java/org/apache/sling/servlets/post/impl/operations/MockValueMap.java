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
package org.apache.sling.servlets.post.impl.operations;

import org.apache.sling.api.resource.ModifiableValueMap;

import java.util.HashMap;

/**
 * Created by ieb on 10/08/2016.
 */
public class MockValueMap extends HashMap<String, Object> implements ModifiableValueMap {
    public MockValueMap() {
    }

    @Override
    public <T> T get(String s, Class<T> aClass) {
        return (T) super.get(s);
    }

    @Override
    public <T> T get(String s, T t) {
        return (T) super.get(s);
    }
}
