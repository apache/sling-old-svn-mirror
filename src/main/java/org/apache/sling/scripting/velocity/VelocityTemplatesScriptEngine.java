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
package org.apache.sling.scripting.velocity;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.HttpStatusCodeException;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptEngine;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * A ScriptEngine that uses Velocity templates to render a Resource
 */
public class VelocityTemplatesScriptEngine implements SlingScriptEngine {

    public final static String VELOCITY_SCRIPT_EXTENSION = "vlt";

    private final VelocityEngine velocity;

    public VelocityTemplatesScriptEngine() throws SlingException {
        velocity = new VelocityEngine();
        try {
            velocity.init();
        } catch (Exception e) {
            throw new SlingException("Exception in Velocity.init() "
                + e.getMessage(), e);
        }
    }

    public String[] getExtensions() {
        return new String[] { VELOCITY_SCRIPT_EXTENSION };
    }

    public String getEngineName() {
        return "Velocity Template Script Engine";
    }

    public String getEngineVersion() {
        return "0.9";
    }

    public void eval(SlingScript script, Map<String, Object> props)
            throws SlingException, IOException {

        // ensure get method
        HttpServletRequest request = (HttpServletRequest) props.get(REQUEST);
        if (!"GET".equals(request.getMethod())) {
            throw new HttpStatusCodeException(
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Velocity templates only support GET requests");
        }

        // initialize the Velocity context
        final VelocityContext c = new VelocityContext();
        for (Entry<String, Object> entry : props.entrySet()) {
            c.put(entry.getKey(), entry.getValue());
        }

        // let Velocity evaluate the script, and send the output to the browser
        final String logTag = getClass().getSimpleName();
        try {
            Writer w = ((HttpServletResponse) props.get(RESPONSE)).getWriter();
            velocity.evaluate(c, w, logTag, script.getScriptReader());
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable t) {
            throw new SlingException("Failure running script "
                + script.getScriptResource().getURI(), t);
        }
    }
}
