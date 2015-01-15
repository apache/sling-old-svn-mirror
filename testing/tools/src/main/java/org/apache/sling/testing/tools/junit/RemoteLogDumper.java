/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.testing.tools.junit;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.testing.tools.http.Request;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.apache.sling.testing.tools.sling.SlingInstanceState;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

/**
 * The RemoteLogDumper Rule fetches logs which are generated due to execution of test from the
 * remote server and dumps them locally upon test failure. This simplifies determining failure
 * cause by providing all required data locally. This would be specially useful when running test
 * in CI server where server logs gets cluttered with all other test executions
 *
 * <pre>
 *     public class LoginTestIT {
 *
 *     &#064;Rule
 *     public TestRule logDumper = new RemoteLogDumper();
 *
 *     &#064;Test
 *     public void remoteLogin() {
 *          //Make calls to remote server
 *          assertEquals(&quot;testA&quot;, name.getMethodName());
 *     }
 *
 *     }
 * </pre>
 */
public class RemoteLogDumper extends TestWatcher {
    public static final String TEST_CLASS = "X-Sling-Test-Class";
    public static final String TEST_NAME = "X-Sling-Test-Name";
    /**
     * Path for the org.apache.sling.junit.impl.servlet.TestLogServlet
     */
    static final String SERVLET_PATH = "/system/sling/testlog";

    @Override
    protected void finished(Description description) {
        MDC.remove(TEST_CLASS);
        MDC.remove(TEST_NAME);
    }

    @Override
    protected void starting(Description description) {
        MDC.put(TEST_CLASS, description.getClassName());
        MDC.put(TEST_NAME, description.getMethodName());
    }

    @Override
    protected void failed(Throwable e, Description description) {
        final String baseUrl = getServerBaseUrl();
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        if (baseUrl != null) {
            try {
                warnIfNopMDCAdapterBeingUsed();
                DefaultHttpClient httpClient = new DefaultHttpClient();
                RequestExecutor executor = new RequestExecutor(httpClient);
                RequestBuilder rb = new RequestBuilder(baseUrl);

                Request r = rb.buildGetRequest(SERVLET_PATH,
                        TEST_CLASS, description.getClassName(),
                        TEST_NAME, description.getMethodName());

                executor.execute(r);
                int statusCode = executor.getResponse().getStatusLine().getStatusCode();

                String msg = e.getMessage();
                if (msg != null) {
                    pw.println(msg);
                }

                if (statusCode == 200){
                    pw.printf("=============== Logs from server [%s] for [%s]===================%n",
                            baseUrl, description.getMethodName());
                    pw.print(executor.getContent());
                    pw.println("========================================================");
                } else {
                    pw.printf("Not able to fetch logs from [%s%s]. " +
                            "TestLogServer probably not configured %n", baseUrl, SERVLET_PATH);
                }

            } catch (Throwable t) {
                System.err.printf("Error occurred while fetching test logs from server [%s] %n", baseUrl);
                t.printStackTrace(System.err);
            }

            System.err.print(sw.toString());
        }
    }

    private static void warnIfNopMDCAdapterBeingUsed() {
        try {
            MDCAdapter adapter = MDC.getMDCAdapter();
            String msg = null;
            if (adapter == null) {
                msg = "No MDC Adapter found.";
            } else if ("org.slf4j.helpers.NOPMDCAdapter".equals(adapter.getClass().getName())) {
                msg = "MDC adapter set to [org.slf4j.helpers.NOPMDCAdapter].";
            }

            if (msg != null) {
                System.err.printf("%s Possibly running with slf4j-simple. " +
                        "Use Logging implementation like Logback to enable proper MDC support so " +
                        "as to make use of RemoteLogDumper feature.%n", msg);
            }

        } catch (Throwable ignore) {

        }
    }

    private static String getServerBaseUrl() {
        SlingInstanceState testState = SlingInstanceState.getInstance(SlingInstanceState.DEFAULT_INSTANCE_NAME);
        String baseUrl = testState.getServerBaseUrl();
        if (testState.isServerReady()) {
            return baseUrl;
        } else if (baseUrl == null) {
            //Running via older HttpTestBase
            baseUrl = removeEndingSlash(System.getProperty("launchpad.http.server.url"));
        }

        if (baseUrl == null){
            baseUrl = "http://localhost:8888";
        }
        return baseUrl;
    }

    private static String removeEndingSlash(String str) {
        if(str != null && str.endsWith("/")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }
}
