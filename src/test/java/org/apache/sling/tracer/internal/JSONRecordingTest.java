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

package org.apache.sling.tracer.internal;

import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

import ch.qos.logback.classic.Level;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.MDC;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JSONRecordingTest {
    static final String MDC_QUERY_ID = "oak.query.id";
    static String QE_LOGGER = "org.apache.jackrabbit.oak.query.QueryImpl";
    private HttpServletRequest request = mock(HttpServletRequest.class);

    private TracerConfig tc = new TracerConfig(TracerContext.QUERY_LOGGER, Level.INFO);

    @Test
    public void logQueries() throws Exception{
        StringWriter sw = new StringWriter();

        when(request.getMethod()).thenReturn("GET");
        JSONRecording r = new JSONRecording("abc", request, true);

        MDC.put(MDC_QUERY_ID, "1");
        r.log(tc, Level.DEBUG, "org.apache.jackrabbit.oak.query.QueryEngineImpl",
                tuple("Parsing {} statement: {}",  "XPATH", "SELECT FOO"));
        r.log(tc, Level.DEBUG, QE_LOGGER, tuple("query plan FOO PLAN"));

        r.done();
        r.render(sw);

        JSONObject json = new JSONObject(sw.toString());
        assertEquals("GET", json.get("method"));
        assertTrue(json.has("time"));
        assertTrue(json.has("timestamp"));
        assertEquals(1, json.getJSONArray("queries").length());
    }

    @Test
    public void requestTrackerLogs() throws Exception{
        StringWriter sw = new StringWriter();
        JSONRecording r = new JSONRecording("abc", request, true);

        r.registerTracker(TestUtil.createTracker("x", "y"));

        r.done();
        r.render(sw);

        JSONObject json = new JSONObject(sw.toString());
        assertEquals(2, json.getJSONArray("requestProgressLogs").length());
    }

    @Test
    public void logs() throws Exception{
        StringWriter sw = new StringWriter();
        JSONRecording r = new JSONRecording("abc", request, true);

        FormattingTuple tp1 = MessageFormatter.arrayFormat("{} is going", new Object[]{"Jack"});
        r.log(tc, Level.INFO, "foo", tp1);
        r.log(tc, Level.WARN, "foo.bar", MessageFormatter.arrayFormat("Jill is going", null));
        r.log(tc, Level.ERROR, "foo.bar",
                MessageFormatter.arrayFormat("Jack and {} is going", new Object[]{"Jill" , new Exception()}));

        r.done();
        r.render(sw);

        JSONObject json = new JSONObject(sw.toString());
        assertEquals(3, json.getJSONArray("logs").length());

        JSONObject l1 = json.getJSONArray("logs").getJSONObject(0);
        assertEquals("INFO", l1.getString("level"));
        assertEquals("foo", l1.getString("logger"));
        assertEquals(tp1.getMessage(), l1.getString("message"));
        assertEquals(1, l1.getJSONArray("params").length());
        assertFalse(l1.has("exception"));
        assertTrue(l1.has("timestamp"));

        JSONObject l3 = json.getJSONArray("logs").getJSONObject(2);
        assertNotNull(l3.get("exception"));
    }

    private static FormattingTuple tuple(String msg){
        return MessageFormatter.format(msg, null);
    }

    private static FormattingTuple tuple(String msg, String ... params){
        return MessageFormatter.arrayFormat(msg, params);
    }
}
