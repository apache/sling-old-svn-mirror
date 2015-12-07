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
package org.apache.sling.pipes;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Execution bindings of a pipe
 */
public class PipeBindings {
    private static final Logger log = LoggerFactory.getLogger(PipeBindings.class);

    public static final String NN_ADDITIONALBINDINGS = "additionalBindings";

    public static final String PN_ADDITIONALSCRIPTS = "additionalScripts";

    ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

    ScriptContext scriptContext = new SimpleScriptContext();

    public static final String PATH_BINDING = "path";

    Map<String, String> pathBindings = new HashMap<>();

    Map<String, Resource> outputResources = new HashMap<>();

    private static final Pattern INJECTED_SCRIPT = Pattern.compile("\\$\\{([^\\}]*)\\}");

    /**
     * public constructor
     */
    public PipeBindings(Resource resource){
        engine.setContext(scriptContext);
        //add path bindings where path.MyPipe will give MyPipe current resource path
        getBindings().put(PATH_BINDING, pathBindings);

        //additional bindings (global variables to use in child pipes expressions)
        Resource additionalBindings = resource.getChild(NN_ADDITIONALBINDINGS);
        if (additionalBindings != null) {
            ValueMap bindings = additionalBindings.adaptTo(ValueMap.class);
            addBindings(bindings);
        }

        Resource scriptsResource = resource.getChild(PN_ADDITIONALSCRIPTS);
        if (scriptsResource != null) {
            String[] scripts = scriptsResource.adaptTo(String[].class);
            if (scripts != null) {
                for (String script : scripts){
                    addScript(resource.getResourceResolver(), script);
                }
            }
        }
    }

    /**
     * add a script file to the engine
     * @param resolver
     * @param path
     */
    public void addScript(ResourceResolver resolver, String path) {
        InputStream is = null;
        try {
            if (path.startsWith("http")) {
                try {
                    URL remoteScript = new URL(path);
                    is = remoteScript.openStream();
                } catch (Exception e) {
                    log.error("unable to retrieve remote script", e);
                }
            } else if (path.startsWith("/")) {
                Resource scriptResource = resolver.getResource(path);
                if (scriptResource != null) {
                    is = scriptResource.adaptTo(InputStream.class);
                }
            }
            if (is != null) {
                try {
                    engine.eval(new InputStreamReader(is), scriptContext);
                } catch (Exception e) {
                    log.error("unable to execute {}", path);
                }
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * adds additional bindings (global variables to use in child pipes expressions)
     * @param bindings
     */
    public void addBindings(Map bindings) {
        log.info("Adding bindings {}", bindings);
        getBindings().putAll(bindings);
    }

    /**
     * Update current resource of a given pipe, and appropriate binding
     * @param pipe
     * @param resource
     */
    public void updateBindings(Pipe pipe, Resource resource) {
        outputResources.put(pipe.getName(), resource);
        if (resource != null) {
            pathBindings.put(pipe.getName(), resource.getPath());
        }
        addBinding(pipe.getName(), pipe.getOutputBinding());
    }

    public void addBinding(String name, Object value){
        getBindings().put(name, value);
    }

    public boolean isBindingDefined(String name){
        return getBindings().containsKey(name);
    }

    public Bindings getBindings() {
        return scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
    }

    /**
     * Doesn't look like nashorn likes template strings :-(
     * @param expr
     * @return
     */
    protected String computeECMA5Expression(String expr){
        StringBuilder expression = new StringBuilder();
        Matcher matcher = INJECTED_SCRIPT.matcher(expr);
        int start = 0;
        while (matcher.find()){
            if (matcher.start() > start){
                if (expression.length() == 0){
                    expression.append("'");
                }
                expression.append(expr.substring(start,matcher.start()));
            }
            if (expression.length() > 0){
                expression.append("' + ");
            }
            expression.append(matcher.group(1));
            start = matcher.end();
            if (start < expr.length()){
                expression.append(" + '");
            }
        }
        if (start < expr.length()){
             expression.append(expr.substring(start) + "'");
        }
        return expression.toString();
    }

    /**
     *
     * @param expr
     * @return
     * @throws ScriptException
     */
    private Object evaluate(String expr) throws ScriptException {
        return engine.eval(computeECMA5Expression(expr), scriptContext);
    }

    /**
     * Expression is a function of variables from execution context, that
     * we implement here as a String
     * @param expr
     * @return
     */
    public String instantiateExpression(String expr){
        try {
            return (String)evaluate(expr);
        } catch (ScriptException e) {
            log.error("Unable to evaluate the script", e);
        }
        return expr;
    }

    /**
     * Instantiate object from expression
     * @param expr
     * @return
     */
    public Object instantiateObject(String expr){
        try {
            Object result = evaluate(expr);
            if (! result.getClass().getName().startsWith("java.lang.")) {
                //special case of the date in which case jdk.nashorn.api.scripting.ScriptObjectMirror will
                //be returned
                JsDate jsDate = ((Invocable) engine).getInterface(result, JsDate.class);
                if (jsDate != null ) {
                    Date date = new Date(jsDate.getTime() + jsDate.getTimezoneOffset() * 60 * 1000);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    return cal;
                }
            }
            return result;
        } catch (ScriptException e) {
            log.error("Unable to evaluate the script for expr {} ", expr, e);
        }
        return expr;
    }

    /**
     *
     * @param name
     * @return
     */
    public Resource getExecutedResource(String name) {
        return outputResources.get(name);
    }

    /**
     * interface mapping a javascript date
     */
    public interface JsDate {
        long getTime();
        int getTimezoneOffset();
    }
}
