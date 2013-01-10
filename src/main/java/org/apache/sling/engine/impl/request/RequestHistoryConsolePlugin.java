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
package org.apache.sling.engine.impl.request;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.request.ResponseUtil;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * Felix OSGi console plugin that displays info about recent requests processed
 * by Sling. Info about all requests can be found in the logs, but this is
 * useful when testing or explaining things.
 */
@SuppressWarnings("serial")
public class RequestHistoryConsolePlugin {

    public static final String LABEL = "requests";

    public static final String INDEX = "index";

    public static final String CLEAR = "clear";

    private static Plugin instance;

    private static ServiceRegistration serviceRegistration;

    public static final int STORED_REQUESTS_COUNT = 20;

    private RequestHistoryConsolePlugin() {
    }

    public static void recordRequest(SlingHttpServletRequest r) {
        if (instance != null) {
            instance.addRequest(r);
        }
    }

    public static void initPlugin(BundleContext context, int maxRequests, List<Pattern> storePatterns) {
        if (instance == null) {
            Plugin tmp = new Plugin(maxRequests, storePatterns);
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_DESCRIPTION,
                "Web Console Plugin to display information about recent Sling requests");
            props.put(Constants.SERVICE_VENDOR,
                "The Apache Software Foundation");
            props.put(Constants.SERVICE_PID, tmp.getClass().getName());
            props.put("felix.webconsole.label", LABEL);
            props.put("felix.webconsole.title", "Recent requests");

            serviceRegistration = context.registerService(
                "javax.servlet.Servlet", tmp, props);
            instance = tmp;
        }
    }

    public static void destroyPlugin() {
        if (instance != null) {
            try {
                if (serviceRegistration != null) {
                    serviceRegistration.unregister();
                    serviceRegistration = null;
                }
            } finally {
                instance = null;
            }
        }
    }

    public static final class Plugin extends HttpServlet {

        private final RequestInfoMap requests;

        private final List<Pattern> storePatterns;

        Plugin(int maxRequests, List<Pattern> storePatterns) {
            this.requests = (maxRequests > 0)
                    ? new RequestInfoMap(maxRequests)
                    : null;
            this.storePatterns = storePatterns;
        }

        public void deactivate() {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }

            clear();
        }

        private void addRequest(SlingHttpServletRequest r) {
            if (requests != null) {
                String requestPath = r.getPathInfo();
                boolean accept = true;
                if (storePatterns != null && storePatterns.size() > 0) {
                    accept = false;
                    for (Pattern pattern : storePatterns) {
                        if (pattern.matcher(requestPath).matches()) {
                            accept = true;
                            break;
                        }
                    }
                }

                if (accept) {
                    synchronized (requests) {
                        RequestInfo info = new RequestInfo(r);
                        requests.put(info.getKey(), info);
                    }
                }
            }
        }

        private void clear() {
            if (requests != null) {
                synchronized (requests) {
                    requests.clear();
                }
            }
        }

        private String getLinksTable(String currentRequestIndex) {
            final List<String> links = new ArrayList<String>();
            if (requests != null) {
                synchronized (requests) {
                    for (RequestInfo info : requests.values()) {
                        final String key = ResponseUtil.escapeXml(info.getKey());
                        final boolean isCurrent = info.getKey().equals(
                            currentRequestIndex);
                        final StringBuilder sb = new StringBuilder();
                        sb.append("<span style='white-space: pre; text-align:right; font-size:80%'>");
                        sb.append(String.format("%1$8s", key));
                        sb.append("</span> ");
                        sb.append("<a href='" + LABEL + "?index=" + key + "'>");
                        if (isCurrent) {
                            sb.append("<b>");
                        }
                        sb.append(ResponseUtil.escapeXml(info.getLabel()));
                        if (isCurrent) {
                            sb.append("</b>");
                        }
                        sb.append("</a> ");
                        links.add(sb.toString());
                    }
                }
            }

            final int nCols = 5;
            while ((links.size() % nCols) != 0) {
                links.add("&nbsp;");
            }

            final StringBuilder tbl = new StringBuilder();

            tbl.append("<table class='nicetable ui-widget'>\n<tr>\n");
            if (links.isEmpty()) {
                tbl.append("No Requests recorded");
            } else {
                int i = 0;
                for (String str : links) {
                    if ((i++ % nCols) == 0) {
                        tbl.append("</tr>\n<tr>\n");
                    }
                    tbl.append("<td>");
                    tbl.append(str);
                    tbl.append("</td>\n");
                }
            }
            tbl.append("</tr>\n");

            tbl.append("</table>\n");
            return tbl.toString();
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // Select request to display
            RequestInfo info = null;
            String key = req.getParameter(INDEX);
            if (key != null && requests != null) {
                synchronized (requests) {
                    info = requests.get(key);
                }
            }

            final PrintWriter pw = resp.getWriter();

            if (requests != null) {
                pw.println("<p class='statline ui-state-highlight'>Recorded "
                    + requests.size() + " requests (max: "
                    + requests.getMaxSize() + ")</p>");
            } else {
                pw.println("<p class='statline ui-state-highlight'>Request Recording disabled</p>");
            }

            pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>");
            pw.println("<span style='float: left; margin-left: 1em'>Recent Requests</span>");
            pw.println("<form method='POST'><input type='hidden' name='clear' value='clear'><input type='submit' value='Clear' class='ui-state-default ui-corner-all'></form>");
            pw.println("</div>");

            pw.println(getLinksTable(key));
            pw.println("<br/>");

            if (info != null) {

                pw.println("<table class='nicetable ui-widget'>");

                // Links to other requests
                pw.println("<thead>");
                pw.println("<tr>");
                pw.printf(
                    "<th class='ui-widget-header'>Request %s (%s %s) by %s - RequestProgressTracker Info</th>%n",
                    key, ResponseUtil.escapeXml(info.getMethod()),
                    ResponseUtil.escapeXml(info.getPathInfo()), ResponseUtil.escapeXml(info.getUser()));
                pw.println("</tr>");
                pw.println("</thead>");

                pw.println("<tbody>");

                // Request Progress Tracker Info
                pw.println("<tr><td>");
                final Iterator<String> it = info.getTracker().getMessages();
                pw.print("<pre>");
                while (it.hasNext()) {
                    pw.print(ResponseUtil.escapeXml(it.next()));
                }
                pw.println("</pre></td></tr>");
                pw.println("</tbody></table>");
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            if (req.getParameter(CLEAR) != null) {
                clear();
                resp.sendRedirect(req.getRequestURI());
            }
        }
    }

    private static class RequestInfo {

        private static AtomicLong requestCounter = new AtomicLong(0);

        private final String key;

        private final String method;

        private final String pathInfo;

        private final String user;

        private final RequestProgressTracker tracker;

        RequestInfo(SlingHttpServletRequest request) {
            this.key = String.valueOf(requestCounter.incrementAndGet());
            this.method = request.getMethod();
            this.pathInfo = request.getPathInfo();
            this.user = request.getRemoteUser();
            this.tracker = request.getRequestProgressTracker();
        }

        public String getKey() {
            return key;
        }

        public String getMethod() {
            return method;
        }

        public String getPathInfo() {
            return pathInfo;
        }

        public String getUser() {
            return user;
        }

        public String getLabel() {
            final StringBuilder sb = new StringBuilder();

            sb.append(getMethod());
            sb.append(' ');

            final String path = getPathInfo();
            if (path != null && path.length() > 0) {
                sb.append(ResourceUtil.getName(getPathInfo()));
            } else {
                sb.append('/');
            }

            return sb.toString();
        }

        public RequestProgressTracker getTracker() {
            return tracker;
        }
    }

    private static class RequestInfoMap extends
            LinkedHashMap<String, RequestInfo> {

        private int maxSize;

        RequestInfoMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(
                java.util.Map.Entry<String, RequestInfo> eldest) {
            return size() > maxSize;
        }

        public int getMaxSize() {
            return maxSize;
        }
    }
}