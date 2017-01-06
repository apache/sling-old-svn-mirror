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

package org.apache.sling.testing.junit.rules;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.interceptors.TestDescriptionHolder;
import org.apache.sling.testing.clients.util.URLParameterBuilder;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.apache.sling.testing.clients.interceptors.TestDescriptionInterceptor.TEST_CLASS_HEADER;
import static org.apache.sling.testing.clients.interceptors.TestDescriptionInterceptor.TEST_NAME_HEADER;

/**
 * The RemoteLogDumper Rule fetches logs which are generated due to execution of test from the
 * remote server and dumps them locally upon test failure. This simplifies determining failure
 * cause by providing all required data locally. This would be specially useful when running test
 * in CI server where server logs gets cluttered with all other test executions
 * Can be constructed either with a {@link SlingClient} or with a {@link Instance}
 *
 * <pre>
 *     public class LoginTestIT {
 *
 *     &#064;Rule
 *     public TestRule logDumper = new RemoteLogDumperRule(slingClient);
 *     // OR public TestRule logDumper = new RemoteLogDumperRule(myInstance);
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
public class RemoteLogDumperRule extends TestWatcher {
    /**
     * Path for the org.apache.sling.junit.impl.servlet.TestLogServlet
     */
    static final String SERVLET_PATH = "/system/sling/testlog";

    private SlingClient slingClient = null;
    private Instance instance = null;

    public RemoteLogDumperRule() {
    }

    public RemoteLogDumperRule (Instance instance) {
        this.instance = instance;
    }

    public RemoteLogDumperRule(SlingClient slingClient) {
        this.slingClient = slingClient;
    }

    public RemoteLogDumperRule setSlingClient(SlingClient slingClient) {
        this.slingClient = slingClient;
        return this;
    }

    public RemoteLogDumperRule setInstance(Instance instance) {
        this.instance = instance;
        return this;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return super.apply(base, description);
    }

    @Override
    protected void finished(Description description) {
        TestDescriptionHolder.removeClassName();
        TestDescriptionHolder.removeMethodName();
    }

    @Override
    protected void starting(Description description) {
        // Get the client from an Instance if it was passed in this way
        if (null == this.slingClient && null != instance) {
            this.slingClient = instance.getAdminClient();
        }
        TestDescriptionHolder.setClassName(description.getClassName());
        TestDescriptionHolder.setMethodName(description.getMethodName());
    }

    @Override
    protected void failed(Throwable e, Description description) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        if (slingClient != null) {
            try {
                SlingHttpResponse response = slingClient.doGet(SERVLET_PATH, URLParameterBuilder.create()
                        .add(TEST_CLASS_HEADER, description.getClassName())
                        .add(TEST_NAME_HEADER, description.getMethodName())
                        .getList(),
                        200);
                String msg = response.getSlingMessage();
                if (msg != null) {
                    pw.println(msg);
                }

                pw.printf("=============== Logs from server [%s] for [%s]===================%n",
                        slingClient.getUrl(), description.getMethodName());
                pw.print(response.getContent());
                pw.println("========================================================");

                System.err.print(sw.toString());
            } catch (Throwable t) {
                System.err.printf("Error occurred while fetching test logs from server [%s] %n", slingClient.getUrl());
                t.printStackTrace(System.err);
            }
        } else {
            System.err.println("No SlingClient configured with the rule");
        }
    }
}
