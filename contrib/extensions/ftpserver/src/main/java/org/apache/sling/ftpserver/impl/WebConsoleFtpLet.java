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
package org.apache.sling.ftpserver.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpStatistics;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;

@SuppressWarnings("serial")
public class WebConsoleFtpLet extends HttpServlet implements Ftplet {

    private FtpStatistics stats;

    private final ConcurrentHashMap<UUID, SessionEntry> sessions = new ConcurrentHashMap<UUID, SessionEntry>();

    public void init(FtpletContext ftpletContext) {
        this.stats = ftpletContext.getFtpStatistics();
    }

    public FtpletResult onConnect(FtpSession session) {
        this.sessions.put(session.getSessionId(), new SessionEntry(session));
        return null;
    }

    public FtpletResult beforeCommand(FtpSession session, FtpRequest request) {
        this.sessions.get(session.getSessionId()).setRequestLine(request.getRequestLine());
        return null;
    }

    public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) {
        this.sessions.get(session.getSessionId()).setRequestLine(null);
        return null;
    }

    public FtpletResult onDisconnect(FtpSession session) {
        this.sessions.remove(session.getSessionId());
        return null;
    }

    public void destroy() {
        // make sure to not hold on to anything after destroyal
        this.sessions.clear();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        final PrintWriter pw = resp.getWriter();

        final Date start = this.stats.getStartTime();
        final long uptime = (System.currentTimeMillis() - start.getTime()) / 1000;
        pw.print("<p class=\"statline ui-state-highlight\">");
        pw.printf("FTP Server active. Started: %s. Uptime %d:%02d:%02d.", start, uptime / 3600, uptime / 60 % 60,
            uptime % 60);
        pw.println("</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<tr>");
        pw.println("<th>Connections</th>");
        pw.println("<th>Anonymous Logins</th>");
        pw.println("<th>Authenticated Logins</th>");
        pw.println("<th>Total</th>");
        pw.println("<th>Failed</th>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<th>Current</th>");
        pw.printf("<td>%d</td>%n", this.stats.getCurrentAnonymousLoginNumber());
        pw.printf("<td>%d</td>%n", this.stats.getCurrentLoginNumber());
        pw.printf("<td>%d</td>%n", this.stats.getCurrentConnectionNumber());
        pw.printf("<td>&nbsp</td>%n");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<th>Total</th>");
        pw.printf("<td>%d</td>%n", this.stats.getTotalAnonymousLoginNumber());
        pw.printf("<td>%d</td>%n", this.stats.getTotalConnectionNumber());
        pw.printf("<td>%d</td>%n", this.stats.getTotalLoginNumber());
        pw.printf("<td>%d</td>%n", this.stats.getTotalFailedLoginNumber());
        pw.println("</tr>");
        pw.println("</table>");

        pw.println("<p>&nbsp;</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<tr>");
        pw.println("<th colspan='2'>Operations</th>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<th>Directory</th>");
        pw.printf("<td>%d created, %d removed</td>%n", this.stats.getTotalDirectoryCreated(),
            this.stats.getTotalDirectoryRemoved());
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<th>File Removals</th>");
        pw.printf("<td>%d</td>%n", this.stats.getTotalDeleteNumber());
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<th>File Uploads</th>");
        pw.printf("<td>%d (%d MB)</td>%n", this.stats.getTotalUploadNumber(),
            this.stats.getTotalUploadSize() / 1024 / 1024);
        pw.println("</tr>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<th>File Downloads</th>");
        pw.printf("<td>%d (%d MB)</td>%n", this.stats.getTotalDownloadNumber(),
            this.stats.getTotalDownloadSize() / 1024 / 1024);
        pw.println("</tr>");
        pw.println("</table>");

        pw.println("<p>&nbsp;</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<tr>");
        pw.println("<th>Session ID</th>");
        pw.println("<th>User</th>");
        pw.println("<th>Address</th>");
        pw.println("<th>Connected</th>");
        pw.println("<th>Logged In</th>");
        pw.println("<th>Last Access</th>");
        pw.println("<th>Max Idle Time (s)</th>");
        pw.println("</tr>");
        for (SessionEntry entry : this.sessions.values()) {
            final FtpSession session = entry.getSession();

            /*
             * Display a subset of the session information. The following
             * is omitted for now:
             *  - session.getDataType();
             *  - session.getFailedLogins();
             *  - session.getFileOffset();
             *  - session.getLanguage()
             *  - session.getUserArgument();
             *  - session.getStructure();
             */

            pw.println("<tr>");
            pw.printf("<td>%s</td>%n", session.getSessionId());
            pw.printf("<td>%s</td>%n", session.getUser().getName());
            pw.printf("<td>%s:%s</td>%n", session.getClientAddress().getAddress().getHostAddress(),
                session.getClientAddress().getPort());
            pw.printf("<td>%1$tF %1$tT</td>%n", session.getConnectionTime());
            pw.printf("<td>%1$tF %1$tT</td>%n", session.getLoginTime());
            pw.printf("<td>%1$tF %1$tT</td>%n", session.getLastAccessTime());
            pw.printf("<td>%d</td>%n", session.getMaxIdleTime());
            pw.println("</tr>");

            if (entry.getRequestLine() != null) {
                pw.println("<tr>");
                pw.printf("<td>&nbsp;</td>%n");
                pw.printf("<td colspan='6'>%s</td>%n", entry.getRequestLine());
                pw.println("</tr>");
            }
        }
        pw.println("</table>");
    }

    private static final class SessionEntry {
        private final FtpSession session;

        private String requestLine;

        public SessionEntry(final FtpSession session) {
            this.session = session;
        }

        public FtpSession getSession() {
            return session;
        }

        public void setRequestLine(String requestLine) {
            this.requestLine = requestLine;
        }

        public String getRequestLine() {
            return requestLine;
        }
    }
}
