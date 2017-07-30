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

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.sling.jcr.contentparser.impl.JsonTicksConverter;
import org.apache.sling.junit.remote.httpclient.RemoteTestHttpClient;
import org.apache.sling.testing.tools.http.RequestCustomizer;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.apache.sling.testing.tools.sling.SlingTestBase;
import org.junit.After;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
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
    private RemoteTestHttpClient testHttpClient;
    private final String username;
    private final String password;
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
        
        // Set configured username using "admin" as default credential
        final String configuredUsername = System.getProperty(SlingTestBase.TEST_SERVER_USERNAME);
        if (configuredUsername != null && configuredUsername.trim().length() > 0) {
            username = configuredUsername;
        } else {
            username = SlingTestBase.ADMIN;
        }

        // Set configured password using "admin" as default credential
        final String configuredPassword = System.getProperty(SlingTestBase.TEST_SERVER_PASSWORD);
        if (configuredPassword != null && configuredPassword.trim().length() > 0) {
            password = configuredPassword;
        } else {
            password = SlingTestBase.ADMIN;
        }
        
        testParameters = (SlingRemoteTestParameters)o;
    }
    
    private void maybeExecuteTests() throws Exception {
        if(testHttpClient != null) {
            // Tests already ran
            return;
        }
        
        testHttpClient = new RemoteTestHttpClient(testParameters.getJunitServletUrl(), this.username, this.password, true);

        // Let the parameters class customize the request if desired 
        if(testParameters instanceof RequestCustomizer) {
            testHttpClient.setRequestCustomizer((RequestCustomizer)testParameters);
        }
        
        // Run tests remotely and get response
        final RequestExecutor executor = testHttpClient.runTests(
                testParameters.getTestClassesSelector(),
                testParameters.getTestMethodSelector(),
                "json"
                );
        executor.assertContentType("application/json");
        final JsonArray json = Json.createReader(new StringReader(JsonTicksConverter.tickToDoubleQuote(executor.getContent()))).readArray();

        // Response contains an array of objects identified by 
        // their INFO_TYPE, extract the tests
        // based on this vlaue
        for(int i = 0 ; i < json.size(); i++) {
            final JsonObject obj = json.getJsonObject(i);
            if(obj.containsKey("INFO_TYPE") && "test".equals(obj.getString("INFO_TYPE"))) {
                children.add(new SlingRemoteTest(testClass, obj));
            }
        }
        
        log.info("Server-side tests executed as {} at {} with path {}",
                new Object[]{this.username, testParameters.getJunitServletUrl(), testHttpClient.getTestExecutionPath()});
        
        // Optionally check that number of tests is as expected
        if(testParameters instanceof SlingTestsCountChecker) {
            ((SlingTestsCountChecker)testParameters).checkNumberOfTests(children.size());
        }
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

    /**
     * Similar to {@link ParentRunner#classBlock} but will call the methods annotated with @After in addition.
     */
    @Override
    protected Statement classBlock(RunNotifier notifier) {
        Statement statement = childrenInvoker(notifier);
        statement = withBeforeClasses(statement);
        // call @After class in addition
        statement = withAfter(statement);
        statement = withAfterClasses(statement);
        return statement;
    }
    
    /**
     * Returns a {@link Statement}: run all non-overridden {@code @After} methods on this class and superclasses after
     * executing {@code statement}; all After methods are always executed: exceptions thrown by previous steps are
     * combined, if necessary, with exceptions from After methods into a {@link MultipleFailureException}.
     */
    private Statement withAfter(Statement statement) {
        List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(After.class);
        return afters.isEmpty() ? statement : new RunAfters(statement, afters, testParameters);
    }
}
