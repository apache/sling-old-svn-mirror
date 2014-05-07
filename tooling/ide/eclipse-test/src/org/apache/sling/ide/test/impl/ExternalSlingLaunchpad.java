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
package org.apache.sling.ide.test.impl;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.rules.ExternalResource;

public class ExternalSlingLaunchpad extends ExternalResource {

    private static final Pattern STARTLEVEL_JSON_SNIPPET = Pattern.compile("\"systemStartLevel\":(\\d+)");
    private static final int EXPECTED_START_LEVEL = 30;
    private static final long MAX_WAIT_TIME_MS = TimeUnit.MINUTES.toMillis(1);

    @Override
    protected void before() throws Throwable {

        int launchpadPort = LaunchpadUtils.getLaunchpadPort();

        Credentials creds = new UsernamePasswordCredentials("admin", "admin");

        HttpClient client = new HttpClient();
        client.getState().setCredentials(new AuthScope("localhost", launchpadPort), creds);
        GetMethod method = new GetMethod("http://localhost:" + launchpadPort + "/system/console/vmstat");

        long cutoff = System.currentTimeMillis() + MAX_WAIT_TIME_MS;

        while (true) {
            int status = client.executeMethod(method);
            if (status == 200) {
                String responseBody = method.getResponseBodyAsString();
                Matcher m = STARTLEVEL_JSON_SNIPPET.matcher(responseBody);
                if (m.find()) {
                    int startLevel = Integer.parseInt(m.group(1));
                    if (startLevel >= EXPECTED_START_LEVEL) {
                        break;
                    }
                }

            }

            if (System.currentTimeMillis() > cutoff) {
                throw new AssertionFailedError("Sling launchpad did not start within " + MAX_WAIT_TIME_MS
                        + " milliseconds");
            }
        }

    }
}
