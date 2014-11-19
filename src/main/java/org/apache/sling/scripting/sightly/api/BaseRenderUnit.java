/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import aQute.bnd.annotation.ProviderType;

/**
 * Compiled version of render units
 */
@ProviderType
public abstract class BaseRenderUnit implements RenderUnit {

    private final Map<String, RenderUnit> subTemplates = new HashMap<String, RenderUnit>();

    private Map<String, RenderUnit> siblings;

    @Override
    public final void render(RenderContext renderContext, Bindings arguments) {
        render(renderContext.getWriter(),
                buildGlobalScope(renderContext.getBindings()),
                new CaseInsensitiveBindings(arguments),
                renderContext.getObjectModel(),
                renderContext.getRuntime(),
                renderContext);
    }

    @Override
    public RenderUnit get(String name) {
        return subTemplates.get(name.toLowerCase());
    }

    @Override
    public Set<String> properties() {
        return subTemplates.keySet();
    }

    protected abstract void render(StackedWriter writer,
                                   Bindings bindings,
                                   Bindings arguments,
                                   ObjectModel dynamic,
                                   SightlyRuntime runtime,
                                   RenderContext renderContext);

    @SuppressWarnings({"unused", "unchecked"})
    protected void callUnit(RenderContext renderContext, Object templateObj, Object argsObj) {
        if (!(templateObj instanceof RenderUnit)) {
            return;
        }
        RenderUnit unit = (RenderUnit) templateObj;
        ObjectModel dynamic = renderContext.getObjectModel();
        Map<String, Object> argumentsMap = dynamic.coerceToMap(argsObj);
        Bindings arguments = new SimpleBindings(Collections.unmodifiableMap(argumentsMap));
        unit.render(renderContext, arguments);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected FluentMap obj() {
        return new FluentMap();
    }

    protected final void addSubTemplate(String name, BaseRenderUnit renderUnit) {
        renderUnit.setSiblings(subTemplates);
        subTemplates.put(name.toLowerCase(), renderUnit);
    }

    private void setSiblings(Map<String, RenderUnit> siblings) {
        this.siblings = siblings;
    }

    private Bindings buildGlobalScope(Bindings bindings) {
        SimpleBindings simpleBindings = new SimpleBindings(bindings);
        simpleBindings.putAll(bindings);
        if (siblings != null) {
            simpleBindings.putAll(siblings);
        }
        simpleBindings.putAll(subTemplates);
        return new CaseInsensitiveBindings(simpleBindings);
    }

    protected static class FluentMap extends HashMap<String, Object> {

        /**
         * Fluent variant of put
         * @param name - the name of the property
         * @param value - the value of the property
         * @return - this instance
         */
        public FluentMap with(String name, Object value) {
            put(name, value);
            return this;
        }

    }

    private static final class CaseInsensitiveBindings extends SimpleBindings {

        private CaseInsensitiveBindings(Map<String, Object> m) {
            for (Map.Entry<String, Object> entry : m.entrySet()) {
                put(entry.getKey().toLowerCase(), entry.getValue());
            }
        }

        @Override
        public Object get(Object key) {
            if (!(key instanceof String)) {
                throw new ClassCastException("key should be a String");
            }
            return super.get(((String) key).toLowerCase());
        }

        @Override
        public boolean containsKey(Object key) {
            if (!(key instanceof String)) {
                throw new ClassCastException("key should be a String");
            }
            return super.containsKey(((String) key).toLowerCase());
        }
    }

}
