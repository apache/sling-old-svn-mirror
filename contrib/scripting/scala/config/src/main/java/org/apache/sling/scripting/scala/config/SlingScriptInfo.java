/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.scala.config;

import static org.apache.sling.scripting.scala.Utils.makeIdentifier;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.scala.AbstractScriptInfo;
import org.apache.sling.scripting.scala.ScriptInfo;

/**
 * This {@link ScriptInfo} implementation provides the script class name
 * by retrieving it from the <code>ScriptContext</code>.
 *
 */
@Component
@Service
public class SlingScriptInfo extends AbstractScriptInfo {

    @Override
    public String getScriptClass(String script, ScriptContext context) throws ScriptException {
        SlingScriptHelper scriptHelper = getScriptHelper(context);

        String path = getRelativePath(scriptHelper.getScript().getScriptResource());
        if (path.endsWith(".scala")) {
            path = path.substring(0, path.length() - 6);
        }

        String[] parts = path.split("/");
        StringBuilder scriptName = new StringBuilder();
        scriptName.append(makeIdentifier(parts[0]));
        for (int k = 1; k < parts.length; k++) {
            scriptName.append('.').append(makeIdentifier(parts[k]));
        }

        return scriptName.toString();
    }

    // -----------------------------------------------------< private >---

    @SuppressWarnings("unchecked")
    private static SlingScriptHelper getScriptHelper(ScriptContext context) throws ScriptException {
        SlingBindings bindings = new SlingBindings();
        bindings.putAll(context.getBindings(ScriptContext.ENGINE_SCOPE));
        SlingScriptHelper scriptHelper = bindings.getSling();
        if (scriptHelper == null) {
            throw new ScriptException("Error retrieving Sling script helper from script context");
        }
        return scriptHelper;
    }

    private static String getRelativePath(Resource scriptResource) {
        String path = scriptResource.getPath();
        String[] searchPath = scriptResource.getResourceResolver().getSearchPath();

        for (int i = 0; i < searchPath.length; i++) {
            if (path.startsWith(searchPath[i])) {
                path = path.substring(searchPath[i].length());
                break;
            }
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

}
