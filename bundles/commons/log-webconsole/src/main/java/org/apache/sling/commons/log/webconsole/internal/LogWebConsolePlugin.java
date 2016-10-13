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

package org.apache.sling.commons.log.webconsole.internal;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.sling.commons.log.logback.webconsole.LogPanel;
import org.apache.sling.commons.log.logback.webconsole.LoggerConfig;
import org.apache.sling.commons.log.logback.webconsole.TailerOptions;
import org.apache.sling.commons.osgi.PropertiesUtil;

import static org.apache.sling.commons.log.logback.webconsole.LogPanel.APP_ROOT;
import static org.apache.sling.commons.log.logback.webconsole.LogPanel.PARAM_APPENDER_NAME;
import static org.apache.sling.commons.log.logback.webconsole.LogPanel.PARAM_TAIL_NUM_OF_LINES;
import static org.apache.sling.commons.log.logback.webconsole.LogPanel.PARAM_TAIL_GREP;
import static org.apache.sling.commons.log.logback.webconsole.LogPanel.PATH_TAILER;

public class LogWebConsolePlugin extends SimpleWebConsolePlugin {
    private static final String RES_LOC = LogPanel.APP_ROOT + "/res/ui";

    private static final String[] CSS_REFS = {
            RES_LOC + "/jquery.autocomplete.css",
            RES_LOC + "/prettify.css",
            RES_LOC + "/log.css",
    };

    private final LogPanel panel;

    public LogWebConsolePlugin(LogPanel panel) {
        super(LogPanel.APP_ROOT, "Log Support", "Sling", CSS_REFS);
        this.panel = panel;
    }

    @Override
    protected void renderContent(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        final PrintWriter pw = resp.getWriter();
        final String consoleAppRoot = getAppRoot(req);

        if (req.getPathInfo() != null) {
            if (req.getPathInfo().endsWith(PATH_TAILER)) {
                String appenderName = req.getParameter(PARAM_APPENDER_NAME);
                String regex = req.getParameter(PARAM_TAIL_GREP);
                addNoSniffHeader(resp);
                if (appenderName == null) {
                    pw.printf("Provide appender name via [%s] request parameter%n", PARAM_APPENDER_NAME);
                    return;
                }
                int numOfLines = PropertiesUtil.toInteger(req.getParameter(PARAM_TAIL_NUM_OF_LINES), 0);
                TailerOptions opts = new TailerOptions(numOfLines, regex);
                panel.tail(pw, appenderName, opts);
                return;
            }
        }

        panel.render(pw, consoleAppRoot);
    }

    @Override
    protected boolean isHtmlRequest(HttpServletRequest request) {
        return !request.getRequestURI().endsWith(PATH_TAILER);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        // check if a configuration should be deleted
        boolean isDelete = req.getParameter("delete") != null;
        // get the configuration pid
        String pid = req.getParameter("pid");
        if (isDelete) {
            // in delete mode remove the logger with the given pid
            panel.deleteLoggerConfig(pid);
        } else {
            // get the logger parameters and configure the logger
            // if the given pid is empty a new logger with be created
            String logger = req.getParameter("logger");
            String logLevel = req.getParameter("loglevel");
            String logFile = req.getParameter("logfile");
            boolean additive = PropertiesUtil.toBoolean(req.getParameter("logAdditive"), false);
            String[] loggers = req.getParameterValues("logger");
            if (null != logger) {
                LoggerConfig config = new LoggerConfig(pid, logLevel, loggers, logFile, additive);
                panel.createLoggerConfig(config);
            }
        }

        // send the redirect back to the logpanel
        final String consoleAppRoot = getAppRoot(req);
        resp.sendRedirect(consoleAppRoot + "/" + APP_ROOT);
    }

    private static String getAppRoot(HttpServletRequest req) {
        return (String) req.getAttribute("felix.webconsole.appRoot");
    }

    private static void addNoSniffHeader(HttpServletResponse resp) {
        resp.setHeader("X-Content-Type-Options", "nosniff");
    }
}
