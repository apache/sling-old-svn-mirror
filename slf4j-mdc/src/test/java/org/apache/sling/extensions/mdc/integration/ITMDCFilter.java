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
package org.apache.sling.extensions.mdc.integration;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.sling.testing.tools.http.Request;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestCustomizer;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.spi.DefaultExamSystem;
import org.ops4j.pax.exam.spi.PaxExamRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ITMDCFilter {
    private static Logger log = LoggerFactory.getLogger(ITMDCFilter.class);
    private static TestContainer testContainer;

    private DefaultHttpClient httpClient = new DefaultHttpClient();
    private RequestExecutor executor = new RequestExecutor(httpClient);

    @Before
    public void startContainer() throws Exception {
        if (testContainer == null) {
            ServerConfiguration sc = new ServerConfiguration();
            ExamSystem system = DefaultExamSystem.create(sc.config());
            testContainer = PaxExamRuntime.createContainer(system);
            testContainer.start();
            new RetryLoop(new RetryLoop.Condition() {
                public String getDescription() {
                    return "Check if MDCTestServlet is up";
                }

                public boolean isTrue() throws Exception {
                    RequestBuilder rb = new RequestBuilder(ServerConfiguration.getServerUrl());
                    executor.execute(rb.buildGetRequest("/mdc")).assertStatus(200);
                    rb = new RequestBuilder(ServerConfiguration.getServerUrl());

                    //Create test config via servlet
                    executor.execute(rb.buildGetRequest("/mdc", "createTestConfig", "true"));
                    TimeUnit.SECONDS.sleep(1);
                    return true;
                }
            },5,100);
        }
    }

    @Test
    public void testDefault() throws Exception{
        RequestBuilder rb = new RequestBuilder(ServerConfiguration.getServerUrl());
        // Add Sling POST options
        RequestExecutor result = executor.execute(
                rb.buildGetRequest("/mdc","foo","bar"));

        JSONObject jb = new JSONObject(result.getContent());
        assertEquals("/mdc", jb.getString("req.requestURI"));
        assertEquals("foo=bar", jb.getString("req.queryString"));
        assertEquals(ServerConfiguration.getServerUrl() + "/mdc", jb.getString("req.requestURL"));
        log.info("Response  {}",result.getContent());
    }

    @Test
    public void testWihCustomData() throws Exception{
        RequestBuilder rb = new RequestBuilder(ServerConfiguration.getServerUrl());

        //Create test config via servlet
        executor.execute(rb.buildGetRequest("/mdc", "createTestConfig", "true"));
        TimeUnit.SECONDS.sleep(1);

        //Pass custom cookie
        BasicClientCookie cookie = new BasicClientCookie("mdc-test-cookie", "foo-test-cookie");
        cookie.setPath("/");
        cookie.setDomain("localhost");
        httpClient.getCookieStore().addCookie(cookie);

        //Execute request
        RequestExecutor result = executor.execute(
                rb.buildGetRequest("/mdc", "mdc-test-param", "foo-test-param", "ignored-param", "ignored-value")
                        .withHeader("X-Forwarded-For", "foo-forwarded-for")
                        .withHeader("mdc-test-header", "foo-test-header")
        );

        JSONObject jb = new JSONObject(result.getContent());
        log.info("Response  {}",result.getContent());

        assertEquals("/mdc", jb.getString("req.requestURI"));
        assertEquals(ServerConfiguration.getServerUrl() + "/mdc", jb.getString("req.requestURL"));
        assertEquals("foo-forwarded-for", jb.getString("req.xForwardedFor"));
        assertEquals("foo-test-header", jb.getString("mdc-test-header"));
        assertEquals("foo-test-param", jb.getString("mdc-test-param"));
        assertEquals("foo-test-cookie", jb.getString("mdc-test-cookie"));

        //Only configured params must be returned
        assertFalse(jb.has("ignored-param"));
    }


    @AfterClass
    public static void stopContainer() {
        if (testContainer != null) {
            testContainer.stop();
            testContainer = null;
        }
    }
}
