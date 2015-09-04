/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.hapi.impl;

import org.apache.sling.hapi.HApiProperty;
import org.apache.sling.hapi.HApiType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractHapiTypeImpl implements HApiType {

    public static final String ABSTRACT = "Abstract";
    private final String name;

    public AbstractHapiTypeImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return ABSTRACT;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getFqdn() {
        return null;
    }

    @Override
    public List<String> getParameters() {
        return null;
    }

    @Override
    public Map<String, HApiProperty> getProperties() {
        return new HashMap<String, HApiProperty>();
    }

    @Override
    public Map<String, HApiProperty> getAllProperties() {
        return getProperties();
    }

    @Override
    public HApiType getParent() {
        return null;
    }

    @Override
    public boolean isAbstract() {
        return true;
    }
}
