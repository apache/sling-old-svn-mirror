/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.webconsole.impl;

import static org.apache.sling.hc.util.FormattingResultLog.msHumanReadable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.request.ResponseUtil;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;

/** Webconsole plugin to execute health check services */
@Component
@Service(Servlet.class)
@SuppressWarnings("serial")
@Properties({
    @Property(name=org.osgi.framework.Constants.SERVICE_DESCRIPTION, value="Apache Sling Health Check Web Console Plugin"),
    @Property(name="felix.webconsole.label", value=HealthCheckWebconsolePlugin.LABEL),
    @Property(name="felix.webconsole.title", value=HealthCheckWebconsolePlugin.TITLE),
    @Property(name="felix.webconsole.category", value=HealthCheckWebconsolePlugin.CATEGORY),
    @Property(name="felix.webconsole.css", value="/healthcheck/res/ui/healthcheck.css")
})
public class HealthCheckWebconsolePlugin extends HttpServlet {

    public static final String TITLE = "Sling Health Check";
    public static final String LABEL = "healthcheck";
    public static final String CATEGORY = "Sling";
    public static final String PARAM_TAGS = "tags";
    public static final String PARAM_DEBUG = "debug";
    public static final String PARAM_QUIET = "quiet";

    public static final String PARAM_FORCE_INSTANT_EXECUTION = "forceInstantExecution";
    public static final String PARAM_COMBINE_TAGS_WITH_OR = "combineTagsWithOr";
    public static final String PARAM_OVERRIDE_GLOBAL_TIMEOUT = "overrideGlobalTimeout";

    @Reference
    private HealthCheckExecutor healthCheckExecutor;

    /** Serve static resource if applicable, and return true in that case */
    private boolean getStaticResource(final HttpServletRequest req, final HttpServletResponse resp)
   throws ServletException, IOException {
        final String pathInfo = req.getPathInfo();
        if (pathInfo!= null && pathInfo.contains("res/ui")) {
            final String prefix = "/" + LABEL;
            final InputStream is = getClass().getResourceAsStream(pathInfo.substring(prefix.length()));
            if (is == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, pathInfo);
            } else {
                final OutputStream os = resp.getOutputStream();
                try {
                    final byte [] buffer = new byte[16384];
                    int n=0;
                    while( (n = is.read(buffer, 0, buffer.length)) > 0) {
                        os.write(buffer, 0, n);
                    }
                } finally {
                    try {
                        is.close();
                    } catch ( final IOException ignore ) {
                        // ignore
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
    throws ServletException, IOException {
        if (getStaticResource(req, resp)) {
            return;
        }

        final String tags = getParam(req, PARAM_TAGS, null);
        final boolean debug = Boolean.valueOf(getParam(req, PARAM_DEBUG, "false"));
        final boolean quiet = Boolean.valueOf(getParam(req, PARAM_QUIET, "false"));
        final boolean combineTagsWithOr = Boolean.valueOf(getParam(req, PARAM_COMBINE_TAGS_WITH_OR, "false"));
        final boolean forceInstantExecution = Boolean.valueOf(getParam(req, PARAM_FORCE_INSTANT_EXECUTION, "false"));
        final String overrideGlobalTimeoutStr = getParam(req, PARAM_OVERRIDE_GLOBAL_TIMEOUT, "");

        final PrintWriter pw = resp.getWriter();
        doForm(pw, tags, debug, quiet, combineTagsWithOr, forceInstantExecution, overrideGlobalTimeoutStr);

        // Execute health checks only if tags are specified (even if empty)
        if (tags != null) {
            HealthCheckExecutionOptions options = new HealthCheckExecutionOptions();
            options.setCombineTagsWithOr(combineTagsWithOr);
            options.setForceInstantExecution(forceInstantExecution);
            try {
                options.setOverrideGlobalTimeout(Integer.valueOf(overrideGlobalTimeoutStr));
            } catch (NumberFormatException nfe) {
                // override not set in UI
            }

            Collection<HealthCheckExecutionResult> results = healthCheckExecutor.execute(options, tags.split(","));

            pw.println("<table class='content healthcheck' cellpadding='0' cellspacing='0' width='100%'>");
            int total = 0;
            int failed = 0;
            for (final HealthCheckExecutionResult exR : results) {

                final Result r = exR.getHealthCheckResult();
                total++;
                if (!r.isOk()) {
                    failed++;
                }
                if (!quiet || !r.isOk()) {
                    renderResult(pw, exR, debug);
                }

            }
            final WebConsoleHelper c = new WebConsoleHelper(resp.getWriter());
            c.titleHtml("Summary", total + " HealthCheck executed, " + failed + " failures");
            pw.println("</table>");
            pw.println("<a href='configMgr/org.apache.sling.hc.core.impl.executor.HealthCheckExecutorImpl'>Configure executor</a><br/><br/>");

        }
    }

    private void renderResult(final PrintWriter pw,
            final HealthCheckExecutionResult exResult,
            final boolean debug)
   throws IOException {
        final Result result = exResult.getHealthCheckResult();
        final WebConsoleHelper c = new WebConsoleHelper(pw);

        final StringBuilder status = new StringBuilder();

        status.append("Tags: ").append(exResult.getHealthCheckMetadata().getTags());
        status.append(" Finished: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(exResult.getFinishedAt()) + " after "
                + msHumanReadable(exResult.getElapsedTimeInMs()));

        c.titleHtml(exResult.getHealthCheckMetadata().getTitle(), null);

        c.tr();
        c.tdContent();
        c.writer().print(ResponseUtil.escapeXml(status.toString()));
        c.writer().print("<br/>Result: <span class='resultOk");
        c.writer().print(result.isOk());
        c.writer().print("'>");
        c.writer().print(result.getStatus().toString());
        c.writer().print("</span>");
        c.closeTd();
        c.closeTr();

        c.tr();
        c.tdContent();
        for(final ResultLog.Entry e : result) {
            if (!debug && e.getStatus().equals(Result.Status.DEBUG)) {
                continue;
            }
            c.writer().print("<div class='log");
            c.writer().print(e.getStatus().toString());
            c.writer().print("'>");
            c.writer().print(e.getStatus().toString());
            c.writer().print(' ');
            c.writer().print(ResponseUtil.escapeXml(e.getMessage()));
            if (e.getException() != null) {
                c.writer().print(" ");
                c.writer().print(ResponseUtil.escapeXml(e.getException().toString()));
            }
            c.writer().println("</div>");
        }
        c.closeTd();
    }

    private void doForm(final PrintWriter pw,
            final String tags,
            final boolean debug,
            final boolean quiet,
            final boolean combineTagsWithOr,
            final boolean forceInstantExecution,
            final String overrideGlobalTimeoutStr)
    throws IOException {
        final WebConsoleHelper c = new WebConsoleHelper(pw);
        pw.print("<form method='get'>");
        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
        c.titleHtml(TITLE, "To execute health check services, enter "
                + " an optional list of tags, to select specific health checks, or no tags for all checks."
                + " Prefix a tag with a minus sign (-) to omit checks having that tag.");

        c.tr();
        c.tdLabel("Health Check tags (comma-separated)");
        c.tdContent();
        c.writer().print("<input type='text' name='" + PARAM_TAGS + "' value='");
        if ( tags != null ) {
            c.writer().print(ResponseUtil.escapeXml(tags));
        }
        c.writer().println("' class='input' size='80'>");
        c.closeTd();
        c.closeTr();

        c.tr();
        c.tdLabel("Combine tags with logical 'OR' instead of the default 'AND'");
        c.tdContent();
        c.writer().print("<input type='checkbox' name='" + PARAM_COMBINE_TAGS_WITH_OR + "' class='input' value='true'");
        if (combineTagsWithOr) {
            c.writer().print(" checked=true");
        }
        c.writer().println(">");
        c.closeTd();
        c.closeTr();

        c.tr();
        c.tdLabel("Show DEBUG logs");
        c.tdContent();
        c.writer().print("<input type='checkbox' name='" + PARAM_DEBUG + "' class='input' value='true'");
        if ( debug ) {
            c.writer().print(" checked=true");
        }
        c.writer().println(">");
        c.closeTd();
        c.closeTr();

        c.tr();
        c.tdLabel("Show failed checks only");
        c.tdContent();
        c.writer().print("<input type='checkbox' name='" + PARAM_QUIET + "' class='input' value='true'");
        if ( quiet ) {
            c.writer().print(" checked=true");
        }
        c.writer().println(">");
        c.closeTd();
        c.closeTr();

        c.tr();
        c.tdLabel("Force instant execution (no cache, async checks are executed)");
        c.tdContent();
        c.writer().print("<input type='checkbox' name='" + PARAM_FORCE_INSTANT_EXECUTION + "' class='input' value='true'");
        if (forceInstantExecution) {
            c.writer().print(" checked=true");
        }
        c.writer().println(">");
        c.closeTd();
        c.closeTr();

        c.tr();
        c.tdLabel("Override global timeout");
        c.tdContent();
        c.writer().print("<input type='text' name='" + PARAM_OVERRIDE_GLOBAL_TIMEOUT + "' value='");
        if (overrideGlobalTimeoutStr != null) {
            c.writer().print(ResponseUtil.escapeXml(overrideGlobalTimeoutStr));
        }
        c.writer().println("' class='input' size='80'>");
        c.closeTd();
        c.closeTr();

        c.tr();
        c.tdContent();
        c.writer().println("<input type='submit' value='Execute selected health checks'/>");
        c.closeTd();
        c.closeTr();

        c.writer().println("</table></form>");
    }

    private String getParam(final HttpServletRequest req, final String name, final String defaultValue) {
        String result = req.getParameter(name);
        if(result == null) {
            result = defaultValue;
        }
        return result;
    }
}
