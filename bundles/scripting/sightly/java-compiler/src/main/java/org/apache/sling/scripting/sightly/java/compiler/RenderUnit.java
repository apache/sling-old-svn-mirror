/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.java.compiler;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.sling.scripting.sightly.Record;
import org.apache.sling.scripting.sightly.render.RenderContext;

/**
 * Basic unit of rendering. This also extends the record interface. The properties for a unit are the sub-units.
 */
public abstract class RenderUnit implements Record<RenderUnit> {

    private final Map<String, RenderUnit> subTemplates = new HashMap<>();

    private Map<String, RenderUnit> siblings;

    /**
     * Render the main script template
     *
     * @param out           the {@link PrintWriter} to which the commands are written
     * @param renderContext the rendering context
     * @param arguments     the arguments for this unit
     */
    public final void render(PrintWriter out, RenderContext renderContext, Bindings arguments) {
        Bindings globalBindings = renderContext.getBindings();
        render(out, buildGlobalScope(globalBindings), new CaseInsensitiveBindings(arguments), renderContext);
    }

    @Override
    public RenderUnit getProperty(String name) {
        return subTemplates.get(name.toLowerCase());
    }

    @Override
    public Set<String> getPropertyNames() {
        return subTemplates.keySet();
    }

    protected abstract void render(PrintWriter out,
                                   Bindings bindings,
                                   Bindings arguments,
                                   RenderContext renderContext);

    @SuppressWarnings({"unused", "unchecked"})
    protected void callUnit(PrintWriter out, RenderContext renderContext, Object templateObj, Object argsObj) {
        if (!(templateObj instanceof RenderUnit)) {
            if (templateObj == null) {
                throw new SightlyJavaCompilerException("data-sly-call: expression evaluates to null.");
            }
            if (renderContext.getObjectModel().isPrimitive(templateObj)) {
                throw new SightlyJavaCompilerException(
                        "data-sly-call: primitive \"" + templateObj.toString() + "\" does not represent a HTL template.");
            } else if (templateObj instanceof String) {
                throw new SightlyJavaCompilerException(
                        "data-sly-call: String '" + templateObj.toString() + "' does not represent a HTL template.");
            }
            throw new SightlyJavaCompilerException(
                    "data-sly-call: " + templateObj.getClass().getName() + " does not represent a HTL template.");
        }
        RenderUnit unit = (RenderUnit) templateObj;
        Map<String, Object> argumentsMap = renderContext.getObjectModel().toMap(argsObj);
        Bindings arguments = new SimpleBindings(Collections.unmodifiableMap(argumentsMap));
        unit.render(out, renderContext, arguments);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected FluentMap obj() {
        return new FluentMap();
    }

    @SuppressWarnings("unused")
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
         * Fluent variant of put.
         *
         * @param name  the name of the property
         * @param value the value of the property
         * @return this instance
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
