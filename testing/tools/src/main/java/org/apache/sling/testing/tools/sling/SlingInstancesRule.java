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
package org.apache.sling.testing.tools.sling;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 *  JUnit Rule that executes tests for multiple Sling instances.
 */
public class SlingInstancesRule implements TestRule {

    private SlingInstance currentInstance;
    private final Iterable<SlingInstance> instances;
    
    public SlingInstancesRule(String ... instanceNames) {
        this(new SlingInstanceManager(instanceNames));
    }
    
    public SlingInstancesRule(Iterable<SlingInstance> it) {
        instances = it;
    }

    /** Evaluate our base statement once for every instance.
     *  Tests can use our getSlingInstance() method to access the current one.
     *  See MultipleOsgiConsoleTest example in the samples integration tests module.
     */
    public Statement apply(final Statement base, Description dest) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                for(SlingInstance instance : instances) {
                    currentInstance = instance;
                    base.evaluate();
               }
                currentInstance = null;
            }
        };
    }
    
    public SlingInstance getSlingInstance() {
        return currentInstance;
    }
}