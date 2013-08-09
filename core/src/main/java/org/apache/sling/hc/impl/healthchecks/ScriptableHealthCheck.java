/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.impl.healthchecks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.Constants;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that checks a scriptable expression */
@Component(
        name="org.apache.sling.hc.ScriptableHealthCheck",
        configurationFactory=true, 
        policy=ConfigurationPolicy.REQUIRE, 
        metatype=true)
@Service
public class ScriptableHealthCheck implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<String, String> info = new HashMap<String, String>();
    private String expression;
    private String languageExtension;
    private BundleContext bundleContext;
    
    public static final String DEFAULT_LANGUAGE_EXTENSION = "ecma";

    @Property
    public static final String PROP_EXPRESSION = "expression";
    
    @Property
    public static final String PROP_LANGUAGE_EXTENSION = "language.extension";
    
    @Property(cardinality=50)
    public static final String PROP_TAGS = Constants.HC_TAGS;
    
    @Property
    public static final String PROP_NAME = Constants.HC_NAME;
    
    @Property
    public static final String PROP_MBEAN_NAME = Constants.HC_MBEAN_NAME;
    
    @Reference
    private ScriptEngineManager scriptEngineManager;
    
    @Activate
    public void activate(ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
        expression = PropertiesUtil.toString(ctx.getProperties().get(PROP_EXPRESSION), "");
        languageExtension = PropertiesUtil.toString(ctx.getProperties().get(PROP_LANGUAGE_EXTENSION), DEFAULT_LANGUAGE_EXTENSION);
        
        info.put(PROP_EXPRESSION, expression);
        info.put(PROP_LANGUAGE_EXTENSION, languageExtension);
        info.put(Constants.HC_NAME, PropertiesUtil.toString(ctx.getProperties().get(Constants.HC_NAME), ""));
        info.put(Constants.HC_MBEAN_NAME, PropertiesUtil.toString(ctx.getProperties().get(Constants.HC_MBEAN_NAME), ""));
        info.put(Constants.HC_TAGS, 
                Arrays.asList(PropertiesUtil.toStringArray(ctx.getProperties().get(Constants.HC_TAGS), new String[] {})).toString());
        
        log.info("Activated, name={}, languageExtension={}, expression={}", languageExtension, expression);
    }
    
    @Override
    public Result execute(ResultLog log) {
        final Result result = new Result(this, log);
        log.debug("Checking expression [{}], language extension=[{}]", expression, languageExtension);
        try {
            final ScriptEngine engine = scriptEngineManager.getEngineByExtension(languageExtension);
            if(engine == null) {
                log.warn("No ScriptEngine available for extension {}", languageExtension);
            } else {
                // TODO pluggable Bindings? Reuse the Sling bindings providers?
                final Bindings b = engine.createBindings();
                b.put("jmx", new JmxScriptBinding(log));
                b.put("osgi", new OsgiScriptBinding(bundleContext, log));
                final Object value = engine.eval(expression, b);
                if(value!=null && "true".equals(value.toString())) {
                    log.debug("Expression [{}] evaluates to true as expected", expression);
                } else {
                    log.warn("Expression [{}] does not evaluate to true, value={}", expression, value);
                }
            }
        } catch(Exception e) {
            log.warn(e.toString(), e);
        }
        return result;
    }

    @Override
    public Map<String, String> getInfo() {
        return info;
    }
}