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
package org.apache.sling.muppet.api;

import org.apache.sling.muppet.util.DefaultEvaluator;

/** Groups a {@link SystemAttribute}, {@link Evaluator} and
 *  String expression to be able to check that the attribute's
 *  value matches the supplied expression.
 */
public class Rule {
    private final SystemAttribute attribute;
    private final Evaluator evaluator;
    private final String expression;
    
    public Rule(SystemAttribute attr, String expression) {
        this(attr, expression, new DefaultEvaluator());
    }
    
    public Rule(SystemAttribute attr, String expression, Evaluator e) {
        this.attribute = attr;
        this.expression = expression;
        this.evaluator = e;
    }
    
    public EvaluationResult.Status execute() {
        return evaluator.evaluate(attribute, expression);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + attribute + " " + expression;
    }
}
