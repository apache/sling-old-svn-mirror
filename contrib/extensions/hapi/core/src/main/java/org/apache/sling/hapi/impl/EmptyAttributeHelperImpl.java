/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.hapi.impl;

import org.apache.sling.hapi.MicrodataAttributeHelper;

import java.util.Collections;
import java.util.Map;

public class EmptyAttributeHelperImpl implements MicrodataAttributeHelper {
    @Override
    public String itemtype() {
        return "";
    }

    @Override
    public Map<String, String> itemtypeMap() {
        return Collections.emptyMap();
    }

    @Override
    public String itemprop(String propName) {
        return "";
    }

    @Override
    public String itemprop(String propName, boolean withType) {
        return "";
    }

    @Override
    public Map<String, String> itempropMap(String propName, boolean withType) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Map<String, String>> allItemPropMap() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> allPropTypesMap() {
        return Collections.emptyMap();
    }
}
