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

import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * A ScriptEngine that uses Velocity templates to render a Resource.
 */
public class VelocityTemplatesScriptEngine extends AbstractSlingScriptEngine {

    private final VelocityEngine velocity;

    public VelocityTemplatesScriptEngine(ScriptEngineFactory factory) {
        super(factory);
        
        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
	        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
	        
	        velocity = new VelocityEngine();
	        
	        // include the Sling resource loader for Velocity
	        Properties props = new Properties();
	        props.put("sling.resource.loader.description", "Sling Resource Loader for Velocity");
	        props.put("sling.resource.loader.class", "org.apache.sling.scripting.velocity.SlingResourceLoader");
	        props.put("sling.resource.loader.cache", "false");
	        props.put("resource.loader","file,sling");
	        
            velocity.init( props );
        } catch (Exception e) {
            throw new RuntimeException("Exception in Velocity.init() "
                + e.getMessage(), e);
	    } finally {
	    	Thread.currentThread().setContextClassLoader(oldClassLoader);
	    }

    }

    public Object eval(Reader script, ScriptContext scriptContext)
            throws ScriptException {
		if (script == null) {
			throw new IllegalArgumentException(
					"Method argument script must not be null.");
		}
		if (scriptContext == null) {
			throw new IllegalArgumentException(
					"Method argument scriptContext must not be null.");
		}
        Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        SlingScriptHelper helper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
        if (helper == null) {
            throw new ScriptException("SlingScriptHelper missing from bindings");
        }

        String scriptName = helper.getScript().getScriptResource().getPath();

        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        
        try {
        	Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        	
	        // initialize the Velocity context
	        final VelocityContext c = new VelocityContext();
	        for (Object entryObj : bindings.entrySet()) {
	            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObj;
	            c.put((String) entry.getKey(), entry.getValue());
	        }
	
	        // let Velocity evaluate the script, and send the output to the browser
	        final String logTag = getClass().getSimpleName();
	        Writer w = scriptContext.getWriter();
	        try {
	            velocity.evaluate(c, w, logTag, script);
	        } catch (Throwable t) {
	            throw new ScriptException("Failure running script " + scriptName
	                + ": " + t + ", stack trace: " + t.getStackTrace() );
	        }
	    } finally {
	    	Thread.currentThread().setContextClassLoader(oldClassLoader);
	    }

        return null;
    }

}
