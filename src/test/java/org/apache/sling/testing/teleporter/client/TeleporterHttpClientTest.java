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
package org.apache.sling.testing.teleporter.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class TeleporterHttpClientTest {
    private static final int PORT = 1234;
    private static final String baseUrl = "http://127.0.0.1:" + PORT;
    private static final String TEST_PATH = "/foo";
    
    @Rule
    public WireMockRule http = new WireMockRule(PORT);
    
    private void activateLater(final String path, long delayMsec) {
        TimerTask t = new TimerTask() {
            public void run() {
                givenThat(get(urlEqualTo(path)).willReturn(aResponse().withStatus(200)));
            }
        };
        
        new Timer(true).schedule(t, delayMsec);
    }
    
    @Test
    public void waitForStatusWithLongTimeout() throws MalformedURLException, IOException {
        final TeleporterHttpClient client = new TeleporterHttpClient(baseUrl, TEST_PATH);
        final String testUrl = baseUrl + TEST_PATH;
        
        assertEquals(404, client.getHttpGetStatus(baseUrl + TEST_PATH));
        activateLater(TEST_PATH, 1000);
        client.waitForStatus(testUrl, 200, 2000);
        assertEquals(200, client.getHttpGetStatus(baseUrl + TEST_PATH));
    }
    
    @Test
    public void waitForStatusWithShortTimeout() throws MalformedURLException, IOException {
        final TeleporterHttpClient client = new TeleporterHttpClient(baseUrl, TEST_PATH);
        final String testUrl = baseUrl + TEST_PATH;
        
        assertEquals(404, client.getHttpGetStatus(baseUrl + TEST_PATH));
        activateLater(TEST_PATH, 1000);
        
        try {
            client.waitForStatus(testUrl, 200, 100);
            fail("Expected waitForStatus to timeout");
        } catch(IOException expected) {
        }
    }
}