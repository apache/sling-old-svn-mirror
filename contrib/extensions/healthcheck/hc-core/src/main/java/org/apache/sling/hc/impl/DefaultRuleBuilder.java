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
package org.apache.sling.hc.impl;

import org.apache.sling.hc.api.HealthCheckFacade;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.api.SystemAttribute;
import org.apache.sling.hc.util.DefaultEvaluator;
import org.slf4j.Logger;

/** {@link RuleBuilder} that provides a few default Rules. */
public class DefaultRuleBuilder implements RuleBuilder {

    public static final String NAMESPACE = "healthcheck";
    public static final String RULE_BUILDER_COUNT = "RuleBuilderCount";
    private final HealthCheckFacade facade;
    
    private class RuleBuilderCountAttribute implements SystemAttribute {
        @Override
        public String toString() {
            return RULE_BUILDER_COUNT;
        }
        @Override
        public Object getValue(Logger logger) {
            final int value = facade.getRuleBuilders().size();
            logger.debug("Our facade has {} builders", value);
            return value;
        }
    };
    
    public DefaultRuleBuilder(HealthCheckFacade facade) {
        this.facade = facade;
    }
    
    @Override
    public Rule buildRule(String namespace, String ruleName, String qualifier, String expression) {
        if(!NAMESPACE.equals(namespace)) {
            return null;
        }
        
        if(RULE_BUILDER_COUNT.equals(ruleName)) {
            return new Rule(new RuleBuilderCountAttribute(), expression, new DefaultEvaluator());
        }
        
        return null;
    }
}
