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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.hc.api.EvaluationResult;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleFilter;
import org.apache.sling.hc.api.RulesEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesEngineImpl implements RulesEngine {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final List<Rule> rules = new ArrayList<Rule>();
    
    @Override
    public void addRule(Rule r) {
        rules.add(r);
    }

    @Override
    public void addRules(List<Rule> r) {
        rules.addAll(r);
    }

    @Override
    public List<EvaluationResult> evaluateRules() {
        return evaluateRules(null);
    }
    
    @Override
    public List<EvaluationResult> evaluateRules(RuleFilter filter) {
        final List<EvaluationResult> result = new ArrayList<EvaluationResult>();
        for(Rule r : rules) {
            if(filter != null && !filter.accept(r)) {
                log.debug("Rule {} rejected by {}", r, filter);
                continue;
            }
            result.add(r.evaluate());
        }
        return result;
    }

}
