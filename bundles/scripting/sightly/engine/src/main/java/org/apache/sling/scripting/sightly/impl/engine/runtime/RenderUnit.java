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
package org.apache.sling.scripting.sightly.impl.engine.runtime;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.sightly.Record;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.impl.utils.RenderUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;

/**
 * Basic unit of rendering. This also extends the record interface. The properties for a unit are the sub-units.
 */
public abstract class RenderUnit implements Record<RenderUnit> {

    private final Map<String, RenderUnit> subTemplates = new HashMap<String, RenderUnit>();

    private Map<String, RenderUnit> siblings;

    /**
     * Render the main script template
     * @param renderContext - the rendering context
     * @param arguments - the arguments for this unit
     */
    public final void render(RenderContext renderContext, Bindings arguments) {
        Bindings globalBindings = renderContext.getBindings();
        PrintWriter writer = (PrintWriter) globalBindings.get(SlingBindings.OUT);
        render(writer, buildGlobalScope(globalBindings), new CaseInsensitiveBindings(arguments), (RenderContextImpl) renderContext);
    }

    @Override
    public RenderUnit getProperty(String name) {
        return subTemplates.get(name.toLowerCase());
    }

    @Override
    public Set<String> getPropertyNames() {
        return subTemplates.keySet();
    }

    protected abstract void render(PrintWriter writer,
                                   Bindings bindings,
                                   Bindings arguments,
                                   RenderContextImpl renderContext);

    @SuppressWarnings({"unused", "unchecked"})
    protected void callUnit(RenderContext renderContext, Object templateObj, Object argsObj) {
        if (!(templateObj instanceof RenderUnit)) {
            if (templateObj == null) {
                throw new SightlyException("data-sly-call: expression evaluates to null.");
            }
            if (RenderUtils.isPrimitive(templateObj)) {
                throw new SightlyException("data-sly-call: primitive \"" + templateObj.toString() + "\" does not represent a Sightly " +
                    "template.");
            } else if (templateObj instanceof String) {
                throw new SightlyException("data-sly-call: String '" + templateObj.toString() + "' does not represent a Sightly template.");
            }
            throw new SightlyException("data-sly-call: " + templateObj.getClass().getName() + " does not represent a Sightly template.");
        }
        RenderUnit unit = (RenderUnit) templateObj;
        SlingScriptHelper ssh = (SlingScriptHelper) renderContext.getBindings().get(SlingBindings.SLING);
        Map<String, Object> argumentsMap = RenderUtils.toMap(argsObj);
        Bindings arguments = new SimpleBindings(Collections.unmodifiableMap(argumentsMap));
        unit.render(renderContext, arguments);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected FluentMap obj() {
        return new FluentMap();
    }

    protected final void addSubTemplate(String name, RenderUnit renderUnit) {
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
