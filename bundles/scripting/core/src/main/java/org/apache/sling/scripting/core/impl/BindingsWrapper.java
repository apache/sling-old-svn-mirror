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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class BindingsWrapper implements Serializable {

    private final List<ScriptingVariableModel> baseVariables;

    private final List<ScriptingVariableModel> customVariables;

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

}
