/*
 * Copyright 2006 - 2008 (C) The original author or authors
 *
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
package javax.script;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is copied from the Livetribe 2.0.6 source with one important
 * extension: The Livetribe implementation has the
 * {@link #setGlobalScope(Bindings)} and {@link #getGlobalScope()} methods,
 * which the Sun implementation of this class has {@link #setBindings(Bindings)}
 * and {@link #getBindings()} methods.
 * <p>
 * Another change is to not scan the class loader initially when creating the
 * ScriptEngineManager -- the assumption is that the ScriptEngine registrations
 * are managed inside the framework.
 */
public class ScriptEngineManager {

    private final Map<String, ScriptEngineFactory> registeredByName = new HashMap<String, ScriptEngineFactory>();

    private final Map<String, ScriptEngineFactory> registeredByExtension = new HashMap<String, ScriptEngineFactory>();

    private final Map<String, ScriptEngineFactory> registeredByMimeType = new HashMap<String, ScriptEngineFactory>();

    private Bindings globalScope;

    public ScriptEngineManager() {
        this.globalScope = new SimpleBindings();
    }

    public ScriptEngineManager(
            @SuppressWarnings("unused") ClassLoader classLoader) {
        this();

        // nothing more, we just have this method for API compatibility,
        // but we don't want to read anything from the class loader
    }

    public Bindings getBindings() {
        return globalScope;
    }

    public void setBindings(Bindings globalScope) {
        if (globalScope == null) {
            throw new IllegalArgumentException("Global scope cannot be null.");
        }

        this.globalScope = globalScope;
    }

    /**
     * @deprecated use {@link #getBindings()} instaed. This method is introduced
     *             by the Livetribe JSR-223 implementation and is wrong.
     *
     * @return the bindings
     */
    @Deprecated
    public Bindings getGlobalScope() {
        return getBindings();
    }

    /**
     * @deprecated use {@link #setBindings(Bindings)} instaed. This method is
     *             introduced by the Livetribe JSR-223 implementation and is
     *             wrong.
     */
    @Deprecated
    public void setGlobalScope(Bindings globalScope) {
        setBindings(globalScope);
    }

    public void put(String key, Object value) {
        globalScope.put(key, value);
    }

    public Object get(String key) {
        return globalScope.get(key);
    }

    public ScriptEngine getEngineByName(String shortName) {
        ScriptEngineFactory factory = registeredByName.get(shortName);
        if (factory == null) return null;

        ScriptEngine engine = factory.getScriptEngine();

        engine.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);

        return engine;
    }

    public ScriptEngine getEngineByExtension(String extension) {
        ScriptEngineFactory factory = registeredByExtension.get(extension);
        if (factory == null) return null;

        ScriptEngine engine = factory.getScriptEngine();

        engine.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);

        return engine;
    }

    public ScriptEngine getEngineByMimeType(String mimeType) {
        ScriptEngineFactory factory = registeredByMimeType.get(mimeType);
        if (factory == null) return null;

        ScriptEngine engine = factory.getScriptEngine();

        engine.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);

        return engine;
    }

    public List<ScriptEngineFactory> getEngineFactories() {
        // we don't apply discovery, so nothing is returned here !
        return Collections.emptyList();
    }

    public void registerEngineName(String name, ScriptEngineFactory factory) {
        registeredByName.put(name, factory);
    }

    public void registerEngineMimeType(String type, ScriptEngineFactory factory) {
        registeredByMimeType.put(type, factory);
    }

    public void registerEngineExtension(String extension,
            ScriptEngineFactory factory) {
        registeredByExtension.put(extension, factory);
    }
}