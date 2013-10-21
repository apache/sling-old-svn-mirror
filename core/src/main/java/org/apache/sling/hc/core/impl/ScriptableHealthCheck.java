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
package org.apache.sling.hc.core.impl;
import java.util.HashSet;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that checks a scriptable expression */
@Component(
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE,
        metatype=true,
        label="Apache Sling Scriptable Health Check",
        description="Uses scripted expressions to verify multiple JMX attributes or other values.")
@Properties({
    @Property(name=HealthCheck.NAME,
            label="Name",
            description="Name of this health check."),
    @Property(name=HealthCheck.TAGS, unbounded=PropertyUnbounded.ARRAY,
              label="Tags",
              description="List of tags for this health check, used to select " +
                        "subsets of health checks for execution e.g. by a composite health check."),
    @Property(name=HealthCheck.MBEAN_NAME,
              label="MBean Name",
              description="Name of the MBean to create for this health check. If empty, no MBean is registered.")
})
@Service(value=HealthCheck.class)
public class ScriptableHealthCheck implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private String expression;
    private String languageExtension;

    private static final String DEFAULT_LANGUAGE_EXTENSION = "ecma";

    @Property(label="Expression",
              description="The value of this expression must be \"true\" for this check to be successful.")
    public static final String PROP_EXPRESSION = "expression";

    @Property(value=DEFAULT_LANGUAGE_EXTENSION,
              label="Language Extension",
              description="File extension of the language to use to evaluate the " +
                      "expression, for example \"ecma\" or \"groovy\", asssuming the corresponding script engine " +
                      "is available. By default \"ecma\" is used.")
    public static final String PROP_LANGUAGE_EXTENSION = "language.extension";

    @Reference
    private ScriptEngineManager scriptEngineManager;

    @Reference(
            cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy=ReferencePolicy.DYNAMIC,
            referenceInterface=BindingsValuesProvider.class,
            target="(context=healthcheck)")
    private final Set<BindingsValuesProvider> bindingsValuesProviders = new HashSet<BindingsValuesProvider>();

    @Activate
    protected void activate(ComponentContext ctx) {
        expression = PropertiesUtil.toString(ctx.getProperties().get(PROP_EXPRESSION), "");
        languageExtension = PropertiesUtil.toString(ctx.getProperties().get(PROP_LANGUAGE_EXTENSION), DEFAULT_LANGUAGE_EXTENSION);

        log.debug("Activated scriptable health check name={}, languageExtension={}, expression={}",
                new Object[] {ctx.getProperties().get(HealthCheck.NAME),
                languageExtension, expression});
    }

    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        resultLog.debug("Checking expression [{}], language extension=[{}]",  expression, languageExtension);
        try {
            final ScriptEngine engine = scriptEngineManager.getEngineByExtension(languageExtension);
            if (engine == null) {
                resultLog.healthCheckError("No ScriptEngine available for extension {}", languageExtension);
            } else {
                // Set Bindings, with our ResultLog as a binding first, so that other bindings can use it
                final Bindings b = engine.createBindings();
                b.put(FormattingResultLog.class.getName(), resultLog);
                synchronized (bindingsValuesProviders) {
                    for(BindingsValuesProvider bvp : bindingsValuesProviders) {
                        log.debug("Adding Bindings provided by {}", bvp);
                        bvp.addBindings(b);
                    }
                }
                log.debug("All Bindings added: {}", b.keySet());

                final Object value = engine.eval(expression, b);
                if(value!=null && "true".equals(value.toString().toLowerCase())) {
                    resultLog.debug("Expression [{}] evaluates to true as expected", expression);
                } else {
                    resultLog.warn("Expression [{}] does not evaluate to true as expected, value=[{}]", expression, value);
                }
            }
        } catch (final Exception e) {
            resultLog.healthCheckError(
                    "Exception while evaluating expression [{}] with language extension [{}]: {}",
                    expression, languageExtension, e);
        }
        return new Result(resultLog);
    }

    public void bindBindingsValuesProvider(BindingsValuesProvider bvp) {
        synchronized (bindingsValuesProviders) {
            bindingsValuesProviders.add(bvp);
        }
        log.debug("{} registered: {}", bvp, bindingsValuesProviders);
    }

    public void unbindBindingsValuesProvider(BindingsValuesProvider bvp) {
        synchronized (bindingsValuesProviders) {
            bindingsValuesProviders.remove(bvp);
        }
        log.debug("{} unregistered: {}", bvp, bindingsValuesProviders);
    }
}
