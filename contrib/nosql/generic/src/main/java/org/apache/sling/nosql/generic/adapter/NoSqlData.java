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
package org.apache.sling.nosql.generic.adapter;

import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * Wrapper for properties of a NoSQL document for a given path.
 */
@ProviderType
public final class NoSqlData {

    private final String path;
    private final Map<String,Object> properties;
    
    public NoSqlData(String path, Map<String, Object> properties) {
        this.path = path;
        this.properties = properties;
    }

    public String getPath() {
        return path;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
}
