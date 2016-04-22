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

import java.util.ArrayList;
import java.util.List;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BindingsWrapper implements JSONString {

    private final List<ScriptingVariableModel> baseVariables;

    private final List<ScriptingVariableModel> customVariables;

    private static final Logger LOGGER = LoggerFactory.getLogger(BindingsWrapper.class);

    public BindingsWrapper() {
        this.baseVariables = new ArrayList<ScriptingVariableModel>();
        this.customVariables = new ArrayList<ScriptingVariableModel>();
    }

    public List<ScriptingVariableModel> getBaseVariables() {
        return baseVariables;
    }

    public List<ScriptingVariableModel> getCustomVariables() {
        return customVariables;
    }

    @Override
    public String toJSONString() {
        try {
            JSONArray baseVars = new JSONArray();
            for (ScriptingVariableModel var : baseVariables) {
                baseVars.put(new JSONObject(var.toJSONString()));
            }
            JSONArray customVars = new JSONArray();
            for (ScriptingVariableModel var : customVariables) {
                customVars.put(new JSONObject(var.toJSONString()));
            }
            return new JSONObject().put("baseVariables", baseVars).put("customVariables", customVars).toString();
        } catch (JSONException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        return JSONObject.NULL.toString();
    }

}
