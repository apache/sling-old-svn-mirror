/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.js.impl.jsapi;

import java.util.HashSet;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.scripting.sightly.js.impl.rhino.HybridObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = ProxyAsyncScriptableFactory.class
)
public class ProxyAsyncScriptableFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyAsyncScriptableFactory.class);

    @Reference
    private SlyBindingsValuesProvider slyBindingsValuesProvider = null;

    public void registerProxies(Bindings bindings) {
        slyBindingsValuesProvider.initialise(bindings);
        Bindings bindingsCopy = new SimpleBindings();
        for (String factoryName : slyBindingsValuesProvider.getScriptPaths().keySet()) {
            ShadowScriptableObject shadowScriptableObject = new ShadowScriptableObject(factoryName, bindingsCopy);
            bindings.put(factoryName, shadowScriptableObject);
        }
        bindingsCopy.putAll(bindings);
    }

    class ShadowScriptableObject extends ScriptableObject {

        private String clazz;
        private Bindings bindings;
        private Set<String> scriptNSUse = new HashSet<>();

        public ShadowScriptableObject(String clazz, Bindings bindings) {
            this.clazz = clazz;
            this.bindings = bindings;
        }

        @Override
        public String getClassName() {
            return clazz;
        }

        @Override
        public Object get(String name, Scriptable start) {
            Object object = bindings.get(clazz);
            if (!(object instanceof HybridObject)) {
                slyBindingsValuesProvider.processBindings(bindings);
            }
            HybridObject hybridObject = (HybridObject) bindings.get(clazz);
            if (hybridObject != null) {
                String script = (String) bindings.get(ScriptEngine.FILENAME);
                if (StringUtils.isNotEmpty(script)) {
                    if (scriptNSUse.add(clazz + ":" + script)) {
                        LOGGER.warn(
                            "Script {} uses the deprecated asynchronous API provided by the '{}' namespace. Please refactor the script to" +
                                " use the synchronous API provided by the org.apache.sling.scripting.javascript bundle.", script, clazz);
                    }
                }
                return hybridObject.get(name, start);
            }
            return Undefined.instance;
        }
    }
}
