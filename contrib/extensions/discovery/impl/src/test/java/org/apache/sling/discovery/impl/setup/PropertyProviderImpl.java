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
package org.apache.sling.discovery.impl.setup;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.discovery.PropertyProvider;

public class PropertyProviderImpl implements PropertyProvider {

    private final Map<String, String> properties = new HashMap<String, String>();

    private int getCnt = 0;

    public PropertyProviderImpl() {
        // nothing so far
    }

    public String getProperty(String name) {
        getCnt++;
        return properties.get(name);
    }

    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    public void setGetCnt(int getCnt) {
        this.getCnt = getCnt;
    }

    public int getGetCnt() {
        return getCnt;
    }

}
