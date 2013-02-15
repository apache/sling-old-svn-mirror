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
package org.apache.sling.testing.tools.http;

import org.apache.sling.testing.tools.retry.RetryLoop;
import org.apache.sling.testing.tools.sling.SlingTestBase;

/** Retry a GET on an URL until it returns 200 or 
 *  until this times out.
 */
public class RetryingContentChecker {
    private final RequestExecutor executor;
    private final RequestBuilder builder;
    private final String username;
    private final String password;
    
    public RetryingContentChecker(RequestExecutor executor, RequestBuilder builder) {
        this(executor, builder, null, SlingTestBase.ADMIN);
    }

    public RetryingContentChecker(RequestExecutor executor, RequestBuilder builder, String username, String password) {
        this.executor = executor;
        this.builder = builder;
        if (username != null) {
            this.username = username;
        } else {
            this.username = SlingTestBase.ADMIN;
        }

        if (password != null) {
            this.password = password;
        } else {
            this.password = SlingTestBase.ADMIN;
        }
    }

    /** Check specified path for expected status, or timeout */
    public void check(final String path, final int expectedStatus, int timeoutSeconds, int intervalBetweenrequestsMsec) {
        final RetryLoop.Condition c = new RetryLoop.Condition() {
            public String getDescription() {
                return "Expecting " + path + " to return HTTP status " + expectedStatus;
            }

            public boolean isTrue() throws Exception {
                executor.execute(builder.buildGetRequest(path)
                        .withCredentials(username, password))
                    .assertStatus(expectedStatus);
                return assertMore(executor);
            }
                
        };
        new RetryLoop(c, timeoutSeconds, intervalBetweenrequestsMsec) {
            @Override
            protected void onTimeout() {
                RetryingContentChecker.this.onTimeout();
            }
        };
    }
    
    /** Optionally perform additional tests in retry condition */
    protected boolean assertMore(RequestExecutor executor) throws Exception {
        return true;
    }

    /** Called if a timeout occurs */
    protected void onTimeout() {
    }
}
