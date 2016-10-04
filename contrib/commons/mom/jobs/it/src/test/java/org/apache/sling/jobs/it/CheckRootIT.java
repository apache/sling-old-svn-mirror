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

package org.apache.sling.jobs.it;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.lang.System;


/**
 */
public class CheckRootIT {


    @Rule
    public final RetryRule retryRule = new RetryRule();

    private DefaultHttpClient client;


    @Before
    public void setup() throws IOException {
        client = new DefaultHttpClient();
    }

    @Test
    public void testHttpRoot() throws Exception {
        final HttpUriRequest get = new HttpGet(TestSuiteLauncherIT.crankstartSetup.getBaseUrl());
        HttpResponse response = null;
        long timeout = System.currentTimeMillis() + 60000L;
        boolean found = false;
        try {
            while (System.currentTimeMillis() < timeout) {
                try {
                    response = client.execute(get);
                    if (response.getStatusLine().getStatusCode() == 404) {
                        found = true;
                        break;
                    }
                } catch (HttpHostConnectException e) {
                    Thread.sleep(1000);
                }
            }
            if (!found) {
                Assert.fail("Expected to get 404 from " + get.getURI());
            }
        } finally {
            Models.closeConnection(response);
        }
    }

}
