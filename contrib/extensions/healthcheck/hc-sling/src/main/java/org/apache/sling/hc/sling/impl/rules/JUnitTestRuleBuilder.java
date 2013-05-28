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
package org.apache.sling.hc.sling.impl.rules;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.api.SystemAttribute;
import org.apache.sling.junit.Renderer;
import org.apache.sling.junit.TestSelector;
import org.apache.sling.junit.TestsManager;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;

/** Creates {@link Rule} to execute JUnit tests using the 
 *  org.apache.sling.junit.core services. 
 */
@Component
@Service(value=RuleBuilder.class)
public class JUnitTestRuleBuilder implements RuleBuilder {

    public static final String NAMESPACE = "sling";
    public static final String RULE_NAME = "junit";
    
    public static final String ALL_PASSED = "ALL_TESTS_PASSED";
    
    @Reference
    private TestsManager testsManager;
    
    private static class CustomRunListener extends RunListener {
        
        final List<Failure> failures = new ArrayList<Failure>();
        int nTests;
        private final Logger logger;
        
        CustomRunListener(Logger logger) {
            this.logger = logger;
        }
        
        @Override
        public void testFailure(Failure failure) throws Exception {
            super.testFailure(failure);
            failures.add(failure);
        }

        @Override
        public void testFinished(Description description) throws Exception {
            super.testFinished(description);
            logger.debug("Test executed: {}", description.getDisplayName());
            nTests++;
        }
    }
    
    private static class CustomRenderer implements Renderer {

        private final String extension;
        final List<String> infoLines = new ArrayList<String>();
        private final RunListener listener;
        
        CustomRenderer(RunListener listener, String extension) {
            this.extension = extension;
            this.listener = listener;
        }
        
        @Override
        public boolean appliesTo(TestSelector ts) {
            return true;
        }

        @Override
        public void cleanup() {
        }

        @Override
        public String getExtension() {
            return extension;
        }

        @Override
        public RunListener getRunListener() {
            return listener;
        }

        @Override
        public void info(String role, String info) {
            infoLines.add(info);
        }

        @Override
        public void link(String arg0, String arg1, String arg2) {
        }

        @Override
        public void list(String arg0, Collection<String> arg1) {
        }

        @Override
        public void setup(HttpServletResponse arg0, String arg1) throws IOException, UnsupportedEncodingException {
        }

        @Override
        public void title(int arg0, String arg1) {
        }
    }
    
    private class JUnitTestSystemAttribute implements SystemAttribute {

        private final TestSelector selector;
        private final String extension;
        
        JUnitTestSystemAttribute(TestSelector selector, String extension) {
            this.selector = selector;
            this.extension = extension;
        }
        
        @Override
        public String toString() {
            return selector.toString();
        }
        
        @Override
        public Object getValue(Logger logger) {
            final CustomRunListener listener = new CustomRunListener(logger);
            final Renderer r = new CustomRenderer(listener, extension);
            final Collection<String> testNames = testsManager.getTestNames(selector);
            if(testNames.isEmpty()) {
                logger.error("No tests found for selector {}", selector);
            } else {
                try {
                    testsManager.executeTests(testNames, r, selector);
                    if(!listener.failures.isEmpty()) {
                        for(Failure f : listener.failures) {
                            logger.warn("Test failed: {}, {}", f.getTestHeader(), f);
                        }
                    } else if(listener.nTests == 0) {
                        logger.warn("No tests executed with selector {}", selector);
                    }
                } catch(Exception e) {
                    logger.error("Exception while executing tests: {}", e.toString());
                }
            }
            
            return null;
        }
    }
    
    @Override
    public Rule buildRule(String namespace, String ruleName, String qualifier, String expression) {
        if(!NAMESPACE.equals(namespace) || !RULE_NAME.equals(ruleName) || qualifier == null) {
            return null;
        }
        
        // Qualifier is the package or class names of JUnit tests to
        // execute. A method can be specified after a hash sign, so
        // for example com.example.tests.MyTestClass#theMethod
        final String [] parts = qualifier.split("#");
        if(parts.length > 0) {
            final String testPackageOrClass = parts[0].trim();
            final String testMethod = parts.length > 1 ? parts[1].trim() : null;
            final String extension = "json";
            final TestSelector selector = new JUnitTestSelector(testPackageOrClass, testMethod, extension);
            return new Rule(new JUnitTestSystemAttribute(selector, extension), expression);
        }
        
        return null;
    }
}
