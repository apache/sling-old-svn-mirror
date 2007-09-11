/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.scripting.Script;
import org.apache.sling.scripting.ScriptHandler;
import org.osgi.service.component.ComponentContext;


/**
 * The <code>ScriptManager</code> TODO
 *
 * @scr.component immediate="true" metatype="false"
 * @scr.property name="service.description" value="Sling ScriptManager"
 * @scr.property name="service.vendor" value="Apache Software Foundation"
 * @scr.reference name="ScriptHandler"
 *                interface="org.apache.sling.scripting.ScriptHandler"
 *                cardinality="0..n" policy="dynamic"
 */
public class ScriptManager {

    private static ScriptManager instance;
    private Map scriptHandlers = new HashMap();

    public static ScriptHandler getScriptHandler(Script script) {
        ScriptManager sm = instance;
        if (sm != null) {

            String type = script.getType();
            if (type == null) {
                String scriptName = script.getScriptName();
                int lastDot = scriptName.lastIndexOf('.');
                type = (lastDot >= 0 && lastDot < scriptName.length() - 1)
                        ? scriptName.substring(lastDot + 1)
                        : scriptName;
            }

            return (ScriptHandler) sm.scriptHandlers.get(type);
        }

        return null;
    }

    //---------- SCR integration ----------------------------------------------

    protected synchronized void activate(ComponentContext context) {
        if (instance == null) {
            instance = this;
        }
    }

    protected synchronized void deactivate(ComponentContext context) {
        if (instance == this) {
            instance = null;
        }
    }

    protected void bindScriptHandler(ScriptHandler scriptHandler) {
        this.scriptHandlers.put(scriptHandler.getType(), scriptHandler);
    }

    protected void unbindScriptHandler(ScriptHandler scriptHandler) {
        // only remove the handler from the map if it is the same
        Object mapped = this.scriptHandlers.get(scriptHandler.getType());
        if (scriptHandler.equals(mapped)) {
            this.scriptHandlers.remove(scriptHandler.getType());
        }
    }
}
