/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class SlingClientWaitExistsTest {
    private static final String GET_WAIT_PATH = "/test/wait/resource";
    private static final String OK_RESPONSE = "TEST_OK";
    private static final String NOK_RESPONSE = "TEST_OK";

    private static int waitCount = 4; // truly randomly chosen by typing with the eyes closed
    private static int callCount = 0;

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() throws IOException {
            serverBootstrap.registerHandler(GET_WAIT_PATH + ".json", new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                    callCount++;
                    if (callCount == waitCount) {
                        response.setEntity(new StringEntity(OK_RESPONSE));
                    } else {
                        response.setEntity(new StringEntity(NOK_RESPONSE));
                        response.setStatusCode(404);
                    }
                }
            });
        }
    };

    @Test
    public void testWaitExists() throws Exception {
        callCount = 0;  // reset counter
        waitCount = 3;  // less than timeout
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        c.waitExists(GET_WAIT_PATH, 500, 10);
        assertEquals(waitCount, callCount);
    }

    @Test
    public void testWaitExistsTimeout() throws Exception {
        callCount = 0;  // reset counter
        waitCount = 40;  // to be sure we reach timeout
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        try {
            c.waitExists(GET_WAIT_PATH, 200, 10);
        } catch (TimeoutException e ) {
            assertTrue("call was executed only " + callCount + " times", callCount > 3);
            return;
        }

        fail("waitExists did not timeout");
    }

    @Test
    public void testWaitExistsOnce() throws Exception {
        callCount = 0;  // reset counter
        waitCount = 1;  // less than timeout
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        c.waitExists(GET_WAIT_PATH, -1, 10);
        assertEquals(1, callCount);
    }
}
