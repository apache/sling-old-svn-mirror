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
package org.apache.sling.scripting.core.impl.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;


public class SlingScriptEngineManager extends ScriptEngineManager {

    private final List<ScriptEngineFactory> factories = new ArrayList<ScriptEngineFactory>();
    private final Map<ScriptEngineFactory, Map<Object, Object>> factoryProperties = new HashMap<ScriptEngineFactory, Map<Object, Object>>();

    public SlingScriptEngineManager(ClassLoader classLoader) {
        super(classLoader);
    }

    public SlingScriptEngineManager() {
        super();
    }

    @Override
    public List<ScriptEngineFactory> getEngineFactories() {
        List<ScriptEngineFactory> baseFactories = super.getEngineFactories();

        List<ScriptEngineFactory> result = new ArrayList<ScriptEngineFactory>();
        result.addAll(factories);
        result.addAll(baseFactories);
        return result;
    }

    public Map<Object, Object> getProperties(ScriptEngineFactory factory) {
        return factoryProperties.get(factory);
    }

    public void registerScriptEngineFactory(ScriptEngineFactory factory, Map<Object, Object> props) {
        for (Object ext : factory.getExtensions()) {
            registerEngineExtension((String) ext, factory);
        }

        for (Object mime : factory.getMimeTypes()) {
            registerEngineMimeType((String) mime, factory);
        }

        for (Object name : factory.getNames()) {
            registerEngineName((String) name, factory);
        }

        factories.add(factory);

        if (props != null) {
            factoryProperties.put(factory, props);
        }
    }

}
