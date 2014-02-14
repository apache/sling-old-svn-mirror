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
package org.apache.sling.launchpad.webapp.integrationtest.util;

import java.io.IOException;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.apache.sling.testing.tools.retry.RetryLoop.Condition;

/** Give access to info provided by the test-services EventsCounterServlet */
public class EventsCounterUtil {
    public static int getEventsCount(HttpTestBase b, String topic) throws JSONException, IOException {
        final JSONObject json = new JSONObject(b.getContent(HttpTest.HTTP_BASE_URL + "/testing/EventsCounter.json", HttpTest.CONTENT_TYPE_JSON));
        return json.has(topic) ? json.getInt(topic) : 0;
    }

    public static void waitForEvent(final HttpTestBase b, final String topic, int timeoutSeconds, final int previousCount) {
        final Condition c = new Condition() {
            public String getDescription() {
                return "Wait for OSGi event on topic " + topic;
            }

            public boolean isTrue() throws Exception {
                return getEventsCount(b, topic) > previousCount;
            }
        };
        new RetryLoop(c, timeoutMsec, 500);
    }
}