/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine;

import java.io.PrintWriter;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderContextImpl;
import org.apache.sling.scripting.sightly.java.compiler.RenderUnit;
import org.apache.sling.scripting.sightly.render.RenderContext;

public class SightlyCompiledScript extends CompiledScript {

    private ScriptEngine scriptEngine;
    private RenderUnit renderUnit;

    public SightlyCompiledScript(ScriptEngine scriptEngine, RenderUnit renderUnit) {
        this.scriptEngine = scriptEngine;
        this.renderUnit = renderUnit;
    }

    @Override
    public Object eval(ScriptContext context) throws ScriptException {
        RenderContext renderContext = new RenderContextImpl(context);
        try {
            PrintWriter out = new PrintWriter(context.getWriter());
            renderUnit.render(out, renderContext, new SimpleBindings());
        } finally {
            renderContext.getBindings().remove(SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER);
        }
        return null;
    }

    @Override
    public ScriptEngine getEngine() {
        return scriptEngine;
    }

    public RenderUnit getRenderUnit() {
        return renderUnit;
    }
}
