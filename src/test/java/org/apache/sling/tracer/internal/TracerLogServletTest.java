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

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.apache.sling.tracer.internal.TestUtil.createTracker;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TracerLogServletTest {

    @Rule
    public final OsgiContext context = new OsgiContext();
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Test
    public void noRecordingByDefault() throws Exception{
        TracerLogServlet logServlet = new TracerLogServlet(context.bundleContext());
        assertSame(Recording.NOOP, logServlet.startRecording(request, response));
        assertSame(Recording.NOOP, logServlet.getRecordingForRequest(request));
    }

    @Test
    public void recordingWhenRequested() throws Exception{
        TracerLogServlet logServlet = new TracerLogServlet(context.bundleContext());
        request = new MockSlingHttpServletRequest();

        Recording recording = logServlet.startRecording(request, response);
        assertNotNull(recording);

        //Once recording is created then it should be returned
        Recording recording2 = logServlet.getRecordingForRequest(request);
        assertSame(recording, recording2);

        //Repeated call should return same recording instance
        Recording recording3 = logServlet.startRecording(request, response);
        assertSame(recording, recording3);

        logServlet.resetCache();

        //If recording gets lost then NOOP must be returned
        Recording recording4 = logServlet.getRecordingForRequest(request);
        assertSame(Recording.NOOP, recording4);
    }

    @Test
    public void jsonRendering() throws Exception{
        TracerLogServlet logServlet = new TracerLogServlet(context.bundleContext());
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader(TracerLogServlet.HEADER_TRACER_RECORDING)).thenReturn("true");

        Recording recording = logServlet.startRecording(request, response);
        recording.registerTracker(createTracker("x" ,"y"));
        recording.done();

        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq(TracerLogServlet.HEADER_TRACER_REQUEST_ID), requestIdCaptor.capture());
        verify(response).setHeader(TracerLogServlet.HEADER_TRACER_PROTOCOL_VERSION,
                String.valueOf(TracerLogServlet.TRACER_PROTOCOL_VERSION));

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        when(request.getRequestURI()).thenReturn("/system/console/" + requestIdCaptor.getValue() + ".json" );

        logServlet.renderContent(request, response);
        JSONObject json = new JSONObject(sw.toString());
        assertEquals("GET", json.getString("method"));
        assertEquals(2, json.getJSONArray("logs").length());
    }

    @Test
    public void pluginRendering() throws Exception{
        TracerLogServlet logServlet = new TracerLogServlet(context.bundleContext());
        when(request.getRequestURI()).thenReturn("/system/console/tracer" );

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        logServlet.renderContent(request, response);

        assertThat(sw.toString(), containsString("Log Tracer"));
    }
}
