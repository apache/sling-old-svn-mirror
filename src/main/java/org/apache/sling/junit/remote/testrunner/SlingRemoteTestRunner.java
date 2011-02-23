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
package org.apache.sling.junit.remote.testrunner;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.http.RequestBuilder;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JUnit TestRunner that talks to a remote
 *  Sling JUnit test servlet. Using this test
 *  runner lets a test class discover tests
 *  that the JUnit servlet can execute, execute
 *  them and report results exactly as if the tests
 *  ran locally.
 */
public class SlingRemoteTestRunner extends ParentRunner<SlingRemoteTest> {
    private static final Logger log = LoggerFactory.getLogger(SlingRemoteTestRunner.class);
    private final SlingRemoteTestParameters testParameters;
    private RequestExecutor executor;
    private RequestBuilder builder;
    private final Class<?> testClass;
    
    private final List<SlingRemoteTest> children = new LinkedList<SlingRemoteTest>();
    
    public SlingRemoteTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        this.testClass = testClass;
        
        Object o = null;
        try {
            o = testClass.newInstance();
            if( !(o instanceof SlingRemoteTestParameters)) {
                throw new IllegalArgumentException(o.getClass().getName() 
                        + " is not a " + SlingRemoteTestParameters.class.getSimpleName());
            }
        } catch(Exception e) {
            throw new InitializationError(e);
        }
        
        testParameters = (SlingRemoteTestParameters)o;
    }
    
    private void maybeExecuteTests() throws Exception {
        if(executor != null) {
            return;
        }
        
        // Setup request execution
        executor = new RequestExecutor(new DefaultHttpClient());
        if(testParameters.getServerBaseUrl() == null) {
            throw new IllegalStateException("Server base URL is null, cannot run tests");
        }
        builder = new RequestBuilder(testParameters.getServerBaseUrl());
        
        // POST request executes the tests
        final Request r = builder.buildPostRequest(testParameters.getJunitServletPath() + "/.json");
        executor.execute(r)
        .assertStatus(200)
        .assertContentType("application/json");
        
        final JSONArray json = new JSONArray(new JSONTokener((executor.getContent())));

        // Response contains an array of objects identified by 
        // their INFO_TYPE, extract the tests
        // based on this vlaue
        for(int i = 0 ; i < json.length(); i++) {
            final JSONObject obj = json.getJSONObject(i);
            if("test".equals(obj.getString("INFO_TYPE"))) {
                children.add(new SlingRemoteTest(testClass, obj));
            }
        }
        
        log.info("{} server-side tests executed at {}", 
                children.size(), testParameters.getJunitServletPath());
        
        // Check that number of tests is as expected
        assertEquals("Expecting " + testParameters.getExpectedNumberOfTests() + " tests",
                testParameters.getExpectedNumberOfTests(),
                children.size());
    }
    
    @Override
    protected Description describeChild(SlingRemoteTest t) {
        return t.describe();
    }

    @Override
    protected List<SlingRemoteTest> getChildren() {
        try {
            maybeExecuteTests();
        } catch(Exception e) {
            throw new Error(e);
        }
        return children;
    }

    @Override
    protected void runChild(SlingRemoteTest t, RunNotifier notifier) {
        try {
            maybeExecuteTests();
        } catch(Exception e) {
            throw new Error(e);
        }
        
        EachTestNotifier eachNotifier= new EachTestNotifier(notifier, t.describe());
        eachNotifier.fireTestStarted();
        try {
            log.debug("Running test {}", t.describe());
            t.run();
        } catch (AssumptionViolatedException e) {
            eachNotifier.addFailedAssumption(e);
        } catch (Throwable e) {
            eachNotifier.addFailure(e);
        } finally {
            eachNotifier.fireTestFinished();
        }
    }
}