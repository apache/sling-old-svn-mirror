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
package org.apache.sling.hc.api;

import java.util.List;

/** An engine that stores and evaluates a list of 
 *  {@link Rule}.
 */
public interface RulesEngine {
    /** Add a rule to this engine */
    void addRule(Rule r);
    
    /** Add a list of rules to this engine */
    void addRules(List<Rule> rules);
    
    /** Evaluate all the current rules.
     *  TODO: we should use tags on rules to group
     *  them in sets (performance, configuration etc.)
     */
    List<EvaluationResult> evaluateRules();
    
    /** Evaluate all rules that the supplied RuleFilter accepts */
    List<EvaluationResult> evaluateRules(RuleFilter filter);
}
