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

import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JSONRecordingTest {
    private HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    public void logQueries() throws Exception{
        StringWriter sw = new StringWriter();

        when(request.getMethod()).thenReturn("GET");
        JSONRecording r = new JSONRecording("abc", request);

        r.log(TracerContext.QUERY_LOGGER, "foo bar", new Object[]{"x" , "y"});
        r.log(TracerContext.QUERY_LOGGER, "foo bar", new Object[]{"x" , "z"});

        r.done();
        r.render(sw);

        JSONObject json = new JSONObject(sw.toString());
        assertEquals("GET", json.get("method"));
        assertEquals(2, json.getJSONArray("queries").length());
    }

    @Test
    public void requestTrackerLogs() throws Exception{
        StringWriter sw = new StringWriter();
        JSONRecording r = new JSONRecording("abc", request);

        r.registerTracker(TestUtil.createTracker("x", "y"));

        r.done();
        r.render(sw);

        JSONObject json = new JSONObject(sw.toString());
        assertEquals(2, json.getJSONArray("logs").length());
    }

}
