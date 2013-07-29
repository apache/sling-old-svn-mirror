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
package org.apache.sling.hc.rules.scriptable;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.sling.hc.api.Evaluator;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.api.SystemAttribute;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;

/** RuleBuilder that builds scriptable rules. The qualifier
 *  indicates the scripting language extension (defaults to ecma) and
 *  the expression must evaluate to true.
 *  The rule name is only used for information, it is not used
 *  in building the rule.
 */
public class ScriptableRuleBuilder implements RuleBuilder {

    public static final String NAMESPACE = "script";
    private final BundleContext ctx;
    
    static private class ScriptedAttribute implements SystemAttribute {

        public static final String DEFAULT_LANGUAGE_EXTENSION = "ecma";
        
        private final String extension;
        private final String expression;
        private final String name;
        private final ScriptEngine scriptEngine;
        private final String scriptEngineMsg;
        
        ScriptedAttribute(BundleContext ctx, String name, String qualifier, String expression) {
            this.extension = qualifier == null || qualifier.length() == 0 ? DEFAULT_LANGUAGE_EXTENSION : qualifier;
            this.expression = expression;
            this.name = name;
            
            // Get a ScriptEngine for our language extension
            final ServiceReference r = ctx.getServiceReference(ScriptEngineManager.class.getName());
            if(r != null) {
                final ScriptEngineManager m = (ScriptEngineManager)ctx.getService(r);
                scriptEngine = m.getEngineByExtension(extension);
                if(scriptEngine == null) {
                    scriptEngineMsg = "ScriptEngine not found for extension '" + extension + "'";
                } else {
                    scriptEngineMsg = null;
                }
            } else {
                scriptEngine = null;
                scriptEngineMsg = "No ScriptEngineManager service available";
            }
            
        }
        
        @Override
        public String toString() {
            return NAMESPACE + ":" + extension + ":" + name;
        }
        
        @Override
        public Object getValue(Logger logger) {
            if(scriptEngineMsg != null) {
                logger.error("Cannot evaluate: {}", scriptEngineMsg);
            } else {
                try {
                    final Bindings b = scriptEngine.createBindings();
                    b.put("jmx", new JmxBinding(logger));
                    return scriptEngine.eval(expression, b);
                } catch(ScriptException e) {
                    logger.error("Script evaluation error [" + expression + "]: " + e, e);
                }
            }
            return null;
        }
    }
    
    static private class ExpectTrueEvaluator implements Evaluator {
        @Override
        public void evaluate(SystemAttribute a, String expression, Logger logger) {
            final Object value = a.getValue(logger);
            if(a instanceof ScriptedAttribute) {
                final ScriptedAttribute sa = (ScriptedAttribute)a;
                logger.debug("({}) [{}] is [{}]", new Object[] { sa.extension, sa.expression, value });
            }
            if(value == null) {
                logger.error("Got null value from {}", a);
            } else if(!Boolean.TRUE.equals(value) && !"true".equals(value.toString())) {
                logger.warn("Expected 'true' value, got '{}' from '{}'", value, a);
            } else {
                logger.debug("Got value '{}' from '{}'", value, a);
            }
        }
    }
    
    public ScriptableRuleBuilder(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Override
    public Rule buildRule(String namespace, String ruleName, final String qualifier, String expression) {
        if(!NAMESPACE.equals(namespace)) {
            return null;
        }
        
        return new Rule(new ScriptedAttribute(ctx, ruleName,qualifier, expression), expression, new ExpectTrueEvaluator());
    }
}