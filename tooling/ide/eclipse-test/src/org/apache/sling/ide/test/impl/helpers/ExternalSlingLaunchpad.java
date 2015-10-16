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
package org.apache.sling.ide.test.impl.helpers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.rules.ExternalResource;

import junit.framework.AssertionFailedError;

public class ExternalSlingLaunchpad extends ExternalResource {

    private static final Pattern STARTLEVEL_JSON_SNIPPET = Pattern.compile("\"systemStartLevel\":(\\d+)");
    private static final int EXPECTED_START_LEVEL = 30;
    private static final long MAX_WAIT_TIME_MS = TimeUnit.MINUTES.toMillis(1);

    private final LaunchpadConfig config;

    public ExternalSlingLaunchpad(LaunchpadConfig config) {
        this.config = config;
    }

    @Override
    protected void before() throws Throwable {

        Credentials creds = new UsernamePasswordCredentials(config.getUsername(), config.getPassword());

        HttpClient client = new HttpClient();
        client.getState().setCredentials(new AuthScope(config.getHostname(), config.getPort()), creds);

        long cutoff = System.currentTimeMillis() + MAX_WAIT_TIME_MS;

        List<SlingReadyRule> rules = new ArrayList<>();
        rules.add(new StartLevelSlingReadyRule(client));
        rules.add(new ActiveBundlesSlingReadyRule(client));

        for (SlingReadyRule rule : rules) {
            while (true) {
                if (rule.evaluate()) {
                    break;
                }
                assertTimeout(cutoff);

                Thread.sleep(100);
            }
        }
    }

    private void assertTimeout(long cutoff) throws AssertionFailedError {
        if (System.currentTimeMillis() > cutoff) {
            throw new AssertionFailedError("Sling launchpad did not start within " + MAX_WAIT_TIME_MS + " milliseconds");
        }
    }

    private void debug(String string) {
        if (System.getProperty("sling.ide.it.debug") != null) {
            System.out.println("[" + new Date() + "] " + string);
        }
    }

    private interface SlingReadyRule {

        boolean evaluate() throws Exception;
    }

    private class StartLevelSlingReadyRule implements SlingReadyRule {

        private final HttpClient client;
        private final GetMethod httpMethod;

        public StartLevelSlingReadyRule(HttpClient client) {
            this.client = client;
            httpMethod = new GetMethod(config.getUrl() + "system/console/vmstat");
        }

        @Override
        public boolean evaluate() throws Exception {

            int status = client.executeMethod(httpMethod);
            debug("vmstat http call got return code " + status);

            if (status == 200) {

                String responseBody = IOUtils.toString(httpMethod.getResponseBodyAsStream(),
                        httpMethod.getResponseCharSet());

                Matcher m = STARTLEVEL_JSON_SNIPPET.matcher(responseBody);
                if (m.find()) {
                    int startLevel = Integer.parseInt(m.group(1));
                    debug("vmstat http call got startLevel " + startLevel);
                    if (startLevel >= EXPECTED_START_LEVEL) {
                        debug("current startLevel " + startLevel + " >= " + EXPECTED_START_LEVEL
                                + ", we are done here");
                        return true;
                    }
                }

            }
            return false;
        }
    }

    private class ActiveBundlesSlingReadyRule implements SlingReadyRule {
        private final HttpClient client;
        private final GetMethod httpMethod;

        public ActiveBundlesSlingReadyRule(HttpClient client) {
            this.client = client;
            httpMethod = new GetMethod(config.getUrl() + "system/console/bundles.json");
        }

        @Override
        public boolean evaluate() throws Exception {
            int status = client.executeMethod(httpMethod);
            debug("bundles http call got return code " + status);
            
            if ( status != 200) {
                return false;
            }

            try ( InputStream input = httpMethod.getResponseBodyAsStream()) {
            
                JSONObject obj = new JSONObject(new JSONTokener(new InputStreamReader(input)));

                JSONArray bundleStatus = obj.getJSONArray("s");

                int total = bundleStatus.getInt(0);
                int active = bundleStatus.getInt(1);
                int fragment = bundleStatus.getInt(2);

                debug("bundle http call status: total = " + total + ", active = " + active + ", fragment = " + fragment);

                if (total == active + fragment) {
                    debug("All bundles are started, we are done here");
                    return true;
                }
            }
            
            return false;
        }
    }
}
