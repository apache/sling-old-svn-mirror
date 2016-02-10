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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.framework.BundleContext;

class TracerLogServlet extends SimpleWebConsolePlugin implements TraceLogRecorder {
    static final String ATTR_REQUEST_ID = TracerLogServlet.class.getName();

    public static final String CLEAR = "clear";

    private static final String LABEL = "tracer";

    public static final String HEADER_TRACER_RECORDING = "Sling-Tracer-Record";

    public static final String HEADER_TRACER_REQUEST_ID = "Sling-Tracer-Request-Id";

    public static final String HEADER_TRACER_PROTOCOL_VERSION = "Sling-Tracer-Protocol-Version";

    public static final int TRACER_PROTOCOL_VERSION = 1;

    private final Cache<String, JSONRecording> cache;

    public TracerLogServlet(BundleContext context) {
        super(LABEL, "Sling Tracer", "Sling", null);
        //TODO Make things configurable
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
        register(context);
    }

    //~-----------------------------------------------< WebConsole Plugin >

    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (isHtmlRequest(request)){
            PrintWriter pw = response.getWriter();
            renderStatus(pw);
            renderRequests(pw);
        } else {
            String requestId = getRequestId(request);
            prepareJSONResponse(response);
            try {
                boolean responseDone = false;
                if (requestId != null) {
                    JSONRecording recording = cache.getIfPresent(requestId);
                    if (recording != null){
                        responseDone = recording.render(response.getOutputStream());
                    }
                }

                if (!responseDone) {
                    PrintWriter pw = response.getWriter();
                    JSONWriter jw = new JSONWriter(pw);
                    jw.object();
                    jw.key("error").value("Not found");
                    jw.endObject();
                }
            } catch (JSONException e) {
                throw new ServletException(e);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (req.getParameter(CLEAR) != null) {
            resetCache();
            resp.sendRedirect(req.getRequestURI());
        }
    }

    @Override
    protected boolean isHtmlRequest(HttpServletRequest request) {
        return request.getRequestURI().endsWith(LABEL);
    }

    private static void prepareJSONResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private void renderStatus(PrintWriter pw) {
        pw.printf("<p class='statline'>Log Tracer Recordings: %d recordings</p>%n", cache.size());

        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>");
        pw.println("<span style='float: left; margin-left: 1em'>Tracer Recordings</span>");
        pw.println("<form method='POST'><input type='hidden' name='clear' value='clear'><input type='submit' value='Clear' class='ui-state-default ui-corner-all'></form>");
        pw.println("</div>");
    }

    private void renderRequests(PrintWriter pw) {
        if (cache.size() > 0){
            pw.println("<ul>");
            for (String id : cache.asMap().keySet()){
                pw.printf("<li><a href='%s/%s.json'>%s</a></li>", LABEL, id, id);
            }
            pw.println("</ul>");
        }

    }

    private static String getRequestId(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        int lastSlash = requestUri.lastIndexOf('/');
        int lastDot = requestUri.indexOf('.', lastSlash + 1);
        if (lastDot > 0){
            return requestUri.substring(lastSlash + 1, lastDot);
        }
        return null;
    }

    //~-----------------------------------------------< TraceLogRecorder >

    @Override
    public Recording startRecording(HttpServletRequest request, HttpServletResponse response) {
        if (request.getHeader(HEADER_TRACER_RECORDING) == null){
            return Recording.NOOP;
        }

        if (request.getAttribute(ATTR_REQUEST_ID) != null){
            //Already processed
            return getRecordingForRequest(request);
        }

        String requestId = generateRequestId();
        JSONRecording recording = record(requestId, request);

        request.setAttribute(ATTR_REQUEST_ID, requestId);

        response.setHeader(HEADER_TRACER_REQUEST_ID, requestId);
        response.setHeader(HEADER_TRACER_PROTOCOL_VERSION, String.valueOf(TRACER_PROTOCOL_VERSION));

        return recording;
    }

    @Override
    public Recording getRecordingForRequest(HttpServletRequest request) {
        String requestId = (String) request.getAttribute(ATTR_REQUEST_ID);
        if (requestId != null){
            return getRecording(requestId);
        }
        return Recording.NOOP;
    }

    Recording getRecording(String requestId) {
        Recording recording = cache.getIfPresent(requestId);
        return recording == null ? Recording.NOOP : recording;
    }

    private JSONRecording record(String requestId, HttpServletRequest request) {
        JSONRecording data = new JSONRecording(requestId, request);
        cache.put(requestId, data);
        return data;
    }

    private static String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    void resetCache(){
        cache.invalidateAll();
    }
}
