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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.sling.testing.clients.util.TimeoutsProvider;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class TeleporterHttpClientTest {
    private static final int PORT = Integer.getInteger("http.port", 1234);
    private static final String baseUrl = "http://127.0.0.1:" + PORT;
    private static final String TEST_PATH = "/foo";
    private static final String username = UUID.randomUUID().toString();
    private static final String password = UUID.randomUUID().toString();
    
    @Rule
    public WireMockRule http = new WireMockRule(PORT);
    
    private void changeStatusLater(final String path, long delayMsec, final int newStatus) {
        TimerTask t = new TimerTask() {
            public void run() {
                http.resetAll();
                http.givenThat(get(urlEqualTo(path)).willReturn(aResponse().withStatus(newStatus)));
            }
        };
        
        new Timer(true).schedule(t, delayMsec);
    }
    
    @Test
    public void waitForStatusWithLongTimeout() throws MalformedURLException, IOException {
        final TeleporterHttpClient client = new TeleporterHttpClient(baseUrl, TEST_PATH);
        final String testUrl = baseUrl + TEST_PATH;
        
        http.givenThat(get(urlEqualTo(TEST_PATH)).willReturn(aResponse().withStatus(418)));
        assertEquals(418, client.getHttpGetStatus(baseUrl + TEST_PATH).getStatus());
        
        changeStatusLater(TEST_PATH, 1000, 200);
        client.waitForStatus(testUrl, 200, TimeoutsProvider.getInstance().getTimeout(2000));
        assertEquals(200, client.getHttpGetStatus(baseUrl + TEST_PATH).getStatus());
    }
    
    @Test
    public void waitForStatusWithShortTimeout() throws MalformedURLException, IOException {
        final TeleporterHttpClient client = new TeleporterHttpClient(baseUrl, TEST_PATH);
        final String testUrl = baseUrl + TEST_PATH;
        
        assertEquals(404, client.getHttpGetStatus(baseUrl + TEST_PATH).getStatus());
        changeStatusLater(TEST_PATH, 1000, 200);
        
        try {
            client.waitForStatus(testUrl, 200, 100);
            fail("Expected waitForStatus to timeout");
        } catch(IOException expected) {
        }
    }
    
    @Test
    public void repeatedGetStatus() {
        final String path = TEST_PATH + "/" + UUID.randomUUID();
        http.givenThat(get(urlEqualTo(path)).willReturn(aResponse().withStatus(200)));
        
        final TeleporterHttpClient client = new TeleporterHttpClient(baseUrl, path);
        final String testUrl = baseUrl + path;
        
        final int N = Integer.getInteger("sling.getstatus.test.count", 1000);
        int status = 0;
        for(int i=0; i < N; i++) {
            try {
                status = client.getHttpGetStatus(testUrl).getStatus();
            } catch(Exception e) {
                fail("Exception at index " + i + ":" + e);
            }
            assertEquals("Expecting status 200 at index " + i, 200, status);
        }
    }
    
    @Test(expected=IllegalStateException.class)
    public void testVerifyCorrectBundleStateForInactiveBundle() throws IOException {
        final TeleporterHttpClient client = new TeleporterHttpClient(baseUrl, "invalid");
        String bundleSymbolicName = "testBundle1";
        // open resource
        try (InputStream inputStream = this.getClass().getResourceAsStream("/bundle-not-active.json")) {
            String body = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            http.givenThat(get(urlEqualTo("/system/console/bundles/" + bundleSymbolicName + ".json")).willReturn(aResponse().withStatus(200).withBody(body)));
        }
        client.verifyCorrectBundleState(bundleSymbolicName, 1);
    }
    
    @Test
    public void testVerifyCorrectBundleStateForActiveBundle() throws IOException {
        final TeleporterHttpClient client = new TeleporterHttpClient(baseUrl, "invalid");
        String bundleSymbolicName = "testBundle2";
        // open resource
        try (InputStream inputStream = this.getClass().getResourceAsStream("/bundle-active.json")) {
            String body = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            http.givenThat(get(urlEqualTo("/system/console/bundles/" + bundleSymbolicName + ".json")).willReturn(aResponse().withStatus(200).withBody(body)));
        }
        client.verifyCorrectBundleState(bundleSymbolicName, 1);
    }
    
    private void testWithCredentials(String path, String credentials, int expectedStatus) throws IOException {
        final TeleporterHttpClient client = new TeleporterHttpClient(baseUrl, "invalid");
        http.givenThat(get(urlEqualTo(path)).willReturn(aResponse().withStatus(418)));
        http.givenThat(get(urlEqualTo(path)).withBasicAuth(username, password).willReturn(aResponse().withStatus(302)));
        client.setCredentials(credentials);
        assertEquals(expectedStatus, client.getHttpGetStatus(baseUrl + path).getStatus());
    }
    
    @Test
    public void testRequiredCredentials() throws IOException {
        testWithCredentials("/protected", username + ":" + password, 302);
    }
    
    @Test
    public void testMissingCredentials() throws IOException {
        testWithCredentials("/protected", null, 418);
    }
}