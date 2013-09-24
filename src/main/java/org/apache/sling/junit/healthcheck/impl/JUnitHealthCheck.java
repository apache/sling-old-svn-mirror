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

package org.apache.sling.junit.healthcheck.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.junit.Renderer;
import org.apache.sling.junit.TestSelector;
import org.apache.sling.junit.TestsManager;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HealthCheck that executes JUnit tests */
@Component(
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE,
        metatype=true)
@Properties({
    @Property(name=HealthCheck.NAME),
    @Property(name=HealthCheck.TAGS, unbounded=PropertyUnbounded.ARRAY),
    @Property(name=HealthCheck.MBEAN_NAME)
})
@Service(value=HealthCheck.class)
public class JUnitHealthCheck implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Reference
    private TestsManager testsManager;
    
    @Property
    public static final String TEST_PACKAGE_OR_CLASS_PROP = "test.package.or.class";
    private TestSelector testSelector;
    
    @Property
    public static final String TEST_METHOD = "test.method";
 
    private static class CustomRunListener extends RunListener {
        
        private final FormattingResultLog resultLog;
        int nTests; 
        
        CustomRunListener(FormattingResultLog resultLog) {
            this.resultLog = resultLog;
        }
        
        @Override
        public void testFailure(Failure failure) throws Exception {
            super.testFailure(failure);
            resultLog.warn(failure.getMessage());
        }

        @Override
        public void testFinished(Description description) throws Exception {
            super.testFinished(description);
            resultLog.debug("Test finished: {}", description);
            nTests++;
        }
    }

    private static class CustomRenderer implements Renderer {

        private final String extension;
        private final RunListener listener;
        private final FormattingResultLog resultLog;
        
        CustomRenderer(RunListener listener, String extension, FormattingResultLog resultLog) {
            this.extension = extension;
            this.listener = listener;
            this.resultLog = resultLog;
        }
        
        public boolean appliesTo(TestSelector ts) {
            return true;
        }

        public void cleanup() {
        }

        public String getExtension() {
            return extension;
        }

        public RunListener getRunListener() {
            return listener;
        }

        public void info(String role, String info) {
            resultLog.info(info);
        }

        public void link(String arg0, String arg1, String arg2) {
        }

        public void list(String arg0, Collection<String> arg1) {
        }

        public void setup(HttpServletResponse arg0, String arg1) throws IOException, UnsupportedEncodingException {
        }

        public void title(int arg0, String arg1) {
        }
    }
    
    @Activate
    public void activate(final Map<String, Object> properties) {
        final String extension = "json";
        final Object o = properties.get(TEST_METHOD);
        final String testMethod = o == null ? "" : o.toString();
        testSelector = new JUnitTestSelector(
                String.valueOf(properties.get(TEST_PACKAGE_OR_CLASS_PROP)), 
                testMethod, 
                extension);
        log.info("Activated with TestSelector={}", testSelector);
    }
    
    public Result execute() {
        final String extension ="json";
        final FormattingResultLog resultLog = new FormattingResultLog();
        final CustomRunListener listener = new CustomRunListener(resultLog);
        final Renderer r = new CustomRenderer(listener, extension, resultLog);
        final Collection<String> testNames = testsManager.getTestNames(testSelector);
        if(testNames.isEmpty()) {
            resultLog.warn("No tests found for selector {}", testSelector);
        } else {
            try {
                testsManager.executeTests(testNames, r, testSelector);
                if(listener.nTests == 0) {
                    resultLog.warn("No tests executed by {}", testSelector);
                }
            } catch(Exception e) {
                resultLog.warn("Exception while executing tests (" + testSelector + ")" + e);
            }
        }
        
        return new Result(resultLog);
    }
    
}
