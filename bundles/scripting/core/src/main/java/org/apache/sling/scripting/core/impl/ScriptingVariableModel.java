/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.core.impl;

import java.util.HashMap;
import java.util.Map;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONString;

class ScriptingVariableModel implements JSONString {

    private final String name;

    private final String description;

    private final String scope;

    private final String jsonString;

    public ScriptingVariableModel(String name, String description, String scope) {
        this.name = name;
        this.description = description;
        this.scope = scope;
        this.jsonString = produceJsonString();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getScope() {
        return scope;
    }

    @Override
    public String toJSONString() {
        return jsonString;
    }

    private String produceJsonString() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", name);
        map.put("description", description);
        map.put("scope", scope);
        return new JSONObject(map).toString();
    }

}
