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
package org.apache.sling.launchpad.webapp.integrationtest.teleporter;

import java.util.Arrays;
import java.util.List;

import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** Rule that executes tests multiple times, including
 *  before/after methods to make sure everything works
 *  fine on the server side 
 */
class GeneratorRule <T> extends ExternalResource {

    private final List<T> values;
    private T currentValue;
    private final StringBuilder trace = new StringBuilder();
    
    static class VerifiableErrorCollector extends ErrorCollector {
        @Override
        public void verify() throws Throwable {
            super.verify();
        }
    }
    
    GeneratorRule (T ... valueArray) {
        values = Arrays.asList(valueArray);
    }
    
    T getValue() {
        return currentValue;
    }
    
    String getTrace() {
        return trace.toString();
    }
    
    @Override
    public Statement apply(final Statement test, Description description) {
        if(!TeleporterRule.isServerSide()) {
            return super.apply(test, description);
        }
        final VerifiableErrorCollector collector = new VerifiableErrorCollector();
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                for(T value : values) {
                    currentValue = value;
                    try {
                        before();
                        test.evaluate();
                        trace.append("-").append(currentValue).append("-");
                        after();
                    } catch(Throwable t) {
                        collector.addError(t);
                    }
                }
                collector.verify();
            }
            
        };
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        trace.append("-before-").append(currentValue);
    }

    @Override
    protected void after() {
        super.after();
        trace.append("-after-").append(currentValue).append("-");
    }
    
}
