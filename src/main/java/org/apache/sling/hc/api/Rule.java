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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.sling.hc.impl.RuleLoggerImpl;
import org.apache.sling.hc.util.DefaultEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Groups a {@link SystemAttribute}, {@link Evaluator} and
 *  String expression to be able to check that the attribute's
 *  value matches the supplied expression.
 */
public class Rule {
    private final SystemAttribute attribute;
    private final Evaluator evaluator;
    private final String expression;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Set<String> tags = Collections.<String>emptySet();
    
    public Rule(SystemAttribute attr, String expression) {
        this(attr, expression, new DefaultEvaluator());
    }
    
    public Rule(SystemAttribute attr, String expression, Evaluator e) {
        this.attribute = attr;
        this.expression = expression;
        this.evaluator = e;
    }
    
    /** Replace the tags of this rule by supplied ones.
     *  Tags are lowercased before being set */
    public void setTags(String ...newTags) {
        tags = new HashSet<String>();
        for(String tag : newTags) {
            tags.add(tag.toLowerCase());
        }
    }
    
    /** Return this rule's tags */
    public Set<String> getTags() {
        return tags; 
    }
    
    /** True if this rule has given tags */
    public boolean hasTag(String tag) {
        return getTags().contains(tag);
    }
    
    /** Evaluate the rule and return the results */
    public EvaluationResult evaluate() {
        final RuleLoggerImpl ruleLogger = new RuleLoggerImpl(logger);
        evaluator.evaluate(attribute, expression, ruleLogger);
        return new EvaluationResult(this, ruleLogger);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " 
            + attribute 
            + (expression == null ? "" : " " + expression);
    }
}
