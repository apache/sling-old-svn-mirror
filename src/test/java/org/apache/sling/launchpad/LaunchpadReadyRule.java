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
package org.apache.sling.launchpad;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.rules.ExternalResource;

public class LaunchpadReadyRule extends ExternalResource {

    private static final int TRIES = 60;
    private static final int WAIT_BETWEEN_TRIES_MILLIS = 1000;

    private final List<Check> checks = new ArrayList<>();

    public LaunchpadReadyRule(int launchpadPort) {

        checks.add(new Check("http://localhost:" + launchpadPort + "/server/default/jcr:root"));
        checks.add(new Check("http://localhost:" + launchpadPort + "/index.html") {
            @Override
            public String runCheck(HttpResponse response) throws Exception {
                try (InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());
                        BufferedReader reader = new BufferedReader(isr)) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("Do not remove this comment, used for Launchpad integration tests")) {
                            return null;
                        }
                    }
                }

                return "Did not find 'ready' marker in the response body";
            }
        });
    }

    @Override
    protected void before() throws Throwable {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            for (Check check : checks) {
                runCheck(client, check);
            }
        }
    }

    private void runCheck(CloseableHttpClient client, Check check) throws Exception {

        String lastFailure = null;
        HttpGet get = new HttpGet(check.getUrl());
        
        for (int i = 0; i < TRIES; i++) {
            try (CloseableHttpResponse response = client.execute(get)) {

                if (response.getStatusLine().getStatusCode() != 200) {
                    lastFailure = "Status code is " + response.getStatusLine();
                    Thread.sleep(WAIT_BETWEEN_TRIES_MILLIS);
                    continue;
                }

                lastFailure = check.runCheck(response);
                if (lastFailure == null) {
                    return;
                }
            }

            Thread.sleep(WAIT_BETWEEN_TRIES_MILLIS);
        }
        
        throw new RuntimeException(String.format("Launchpad not ready. Failed check for URL %s with message '%s'",
                check.getUrl(), lastFailure));
    }

    static class Check {
        private String url;

        public Check(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        /**
         * @param response the HttpResponse
         * @return null if check check was successful, an error description otherwise
         * @throws Exception
         */
        public String runCheck(HttpResponse response) throws Exception {
            return null;
        }
    }

}
