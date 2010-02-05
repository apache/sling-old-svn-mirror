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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.api.SlingHttpServletRequest;
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

    public static final int STORED_REQUESTS_COUNT = 20;

    private RequestHistoryConsolePlugin() {
    }

    public static void recordRequest(SlingHttpServletRequest r) {
        if (instance != null) {
            instance.addRequest(r);
        }
    }

    public static void initPlugin(BundleContext context) {
        if (instance == null) {
            Plugin tmp = new Plugin();
            tmp.activate(context);
            instance = tmp;
        }
    }

    public static void destroyPlugin() {
        if (instance != null) {
            try {
                instance.deactivate();
            } finally {
                instance = null;
            }
        }
    }

    public static final class Plugin extends AbstractWebConsolePlugin {

        private final SlingHttpServletRequest[] requests = new SlingHttpServletRequest[STORED_REQUESTS_COUNT];

        /** Need to store methods separately, apparently requests clear this data when done processing */
        private final String [] methods = new String[STORED_REQUESTS_COUNT];

        private ServiceRegistration serviceRegistration;

        private int lastRequestIndex = -1;

        private synchronized void addRequest(SlingHttpServletRequest r) {
            int index = lastRequestIndex + 1;
            if (index >= requests.length) {
                index = 0;
            }
            requests[index] = r;
            methods[index] = r.getMethod();
            lastRequestIndex = index;
        }

        private synchronized void clear() {
            for(int i=0; i < requests.length; i++) {
                requests[i] = null;
            }
            lastRequestIndex = -1;
        }

        public void activate(BundleContext context) {
            super.activate(context);

            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_DESCRIPTION,
                "Web Console Plugin to display information about recent Sling requests");
            props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
            props.put(Constants.SERVICE_PID, getClass().getName());
            props.put(WebConsoleConstants.PLUGIN_LABEL, LABEL);

            serviceRegistration = context.registerService(
                WebConsoleConstants.SERVICE_NAME, this, props);
        }

        public void deactivate() {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
            super.deactivate();
        }

        @Override
        public String getLabel() {
            return LABEL;
        }

        @Override
        public String getTitle() {
            return "Recent requests";
        }

        private int getArrayIndex(int displayIndex) {
            int result = lastRequestIndex - displayIndex;
            if (result < 0) {
                result += requests.length;
            }
            return result;
        }

        private String getLinksTable(int currentRequestIndex) {
            final List<String> links = new ArrayList<String>();
            for (int i = 0; i < requests.length; i++) {
                final StringBuilder sb = new StringBuilder();
                if (requests[i] != null) {
                    sb.append("<a href='" + LABEL + "?index=" + i + "'>");
                    if (i == currentRequestIndex) {
                        sb.append("<b>");
                    }
                    sb.append(getRequestLabel(getArrayIndex(i)));
                    if (i == currentRequestIndex) {
                        sb.append("</b>");
                    }
                    sb.append("</a> ");
                    links.add(sb.toString());
                }
            }

            final int nCols = 5;
            while((links.size() % nCols) != 0) {
                links.add("&nbsp;");
            }

            final StringBuilder tbl = new StringBuilder();

            tbl.append("<table>\n<tr>\n");
            int i=0;
            for(String str : links) {
                if( (i++ % nCols) == 0) {
                    tbl.append("</tr>\n<tr>\n");
                }
                tbl.append("<td>");
                tbl.append(str);
                tbl.append("</td>\n");
            }
            tbl.append("</tr>\n");

            tbl.append("</table>\n");
            return tbl.toString();
        }

        @Override
        protected void renderContent(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException {

            // If so requested, clear our data
            if(req.getParameter(CLEAR) != null) {
                clear();
                res.sendRedirect(LABEL);
                return;
            }

            // Select request to display
            int index = 0;
            final String tmp = req.getParameter(INDEX);
            if (tmp != null) {
                try {
                    index = Integer.parseInt(tmp);
                } catch (NumberFormatException ignore) {
                    // ignore
                }
            }

            // index is relative to lastRequestIndex
            final int arrayIndex = getArrayIndex(index);

            SlingHttpServletRequest r = null;
            try {
                r = requests[arrayIndex];
            } catch (ArrayIndexOutOfBoundsException ignore) {
                // ignore
            }

            final PrintWriter pw = res.getWriter();

            pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

            // Links to other requests
            pw.println("<thead>");
            pw.println("<tr class='content'>");
            pw.println("<th colspan='2'class='content container'>Recent Requests");
            pw.println(" (<a href='" + LABEL + "?clear=clear'>Clear</a>)");
            pw.println("</th>");
            pw.println("</thead>");
            pw.println("<tbody>");
            pw.println("<tr class='content'><td>");
            pw.println(getLinksTable(index));
            pw.println("</td></tr>");

            if (r != null) {
                // Request Progress Tracker Info
                pw.println("<tr class='content'>");
                pw.println("<th colspan='2'class='content container'>");
                pw.print("Request " + index + " (" + getRequestLabel(index) + ") - RequestProgressTracker Info");
                pw.println("</th></tr>");
                pw.println("<tr><td colspan='2'>");
                final Iterator<String> it = r.getRequestProgressTracker().getMessages();
                pw.print("<pre>");
                while (it.hasNext()) {
                    pw.print(escape(it.next()));
                }
                pw.println("</pre></td></tr>");
            }
            pw.println("</tbody></table>");
        }

        private static String escape(String str) {
            final StringBuilder sb = new StringBuilder();
            for(int i=0; i < str.length(); i++) {
                final char c = str.charAt(i);
                if(c == '<') {
                    sb.append("&lt;");
                } else if (c == '>') {
                    sb.append("&gt;");
                } else if (c == '&') {
                    sb.append("&amp;");
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private String getRequestLabel(int index) {
            final StringBuilder sb = new StringBuilder();
            String path = requests[index].getPathInfo();
            if (path == null) {
                path = "";
            }

            sb.append(methods[index]);
            sb.append(' ');

            final int pos = requests[index].getPathInfo().lastIndexOf('/');
            if(pos < 0) {
                sb.append(requests[index].getPathInfo());
            } else {
                sb.append(requests[index].getPathInfo().substring(pos+1));
            }
            return sb.toString();
        }
    }
}