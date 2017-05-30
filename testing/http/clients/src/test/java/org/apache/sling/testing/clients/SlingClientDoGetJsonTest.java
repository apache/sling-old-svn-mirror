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
import org.codehaus.jackson.JsonNode;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class SlingClientDoGetJsonTest {
    private static final String GET_JSON_PATH = "/test/json/resource";
    private static final String JSON_RESPONSE = "{\"jcr:primaryType\":\"cq:Page\",\"jcr:createdBy\":\"admin-json\"}";
    private static final String JSON_INF_RESPONSE = "{\"jcr:primaryType\":\"cq:Page\",\"jcr:createdBy\":\"admin-infinity\"}";

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() throws IOException {
            serverBootstrap.registerHandler(GET_JSON_PATH + ".1.json", new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                    response.setEntity(new StringEntity(JSON_RESPONSE));
                }
            });

            serverBootstrap.registerHandler(GET_JSON_PATH + ".infinity.json", new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                    response.setEntity(new StringEntity(JSON_INF_RESPONSE));
                }
            });
        }
    };

    @Test
    public void testDoGetJson() throws Exception {
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        JsonNode res = c.doGetJson(GET_JSON_PATH, 1, 200);
        assertEquals("admin-json", res.get("jcr:createdBy").getTextValue());
    }

    @Test
    public void testDoGetJsonInfinity() throws Exception {
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        JsonNode res = c.doGetJson(GET_JSON_PATH, -1, 200);
        assertEquals("admin-infinity", res.get("jcr:createdBy").getTextValue());
    }
}
