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

import org.apache.sling.component.Component;
import org.apache.sling.scripting.ComponentRenderer;
import org.apache.sling.scripting.ScriptHandler;


/**
 * The <code>ComponentRendererScriptHandler</code> class assumes the script
 * name to be a fully qualified name of a class implementing the
 * {@link ComponentRenderer} interface. The class is just loaded and
 * instantiated.
 * 
 * @scr.component immediate="false" metatype="false"
 * @scr.property name="service.description"
 *      value="Sling ComponentRenderer Script Handler"
 * @scr.property name="service.vendor" value="Apache Software Foundation"
 * @scr.service
 */
public class ComponentRendererScriptHandler implements ScriptHandler {

    // TODO: we should probably cache instances of the component renderers instantiated ??
    
    public String getType() {
        return "ComponentRenderer";
    }

    public ComponentRenderer getComponentRenderer(Component component, String scriptName) {
        Object script = getScriptInstance(component, scriptName);
        return (script instanceof ComponentRenderer) ? (ComponentRenderer) script : null;
    }
    
    private Object getScriptInstance(Component component, String scriptName) {
        try {
            // TODO need a proper classloader (RepositoryClassLoader, etc...)
            Class crClass = Class.forName(scriptName, true, getClass().getClassLoader());
            return crClass.newInstance();
        } catch (Throwable t) {
            // TODO: log error
        }
        
        // fall back to nothing
        return null;
    }
}
