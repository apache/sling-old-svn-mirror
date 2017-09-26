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
package org.apache.sling.hc.core.impl.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.Result.Status;
import org.apache.sling.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.apache.sling.hc.api.execution.HealthCheckSelector;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Servlet that triggers the health check executor to return results via http.
 *
 * Parameters:
 * <ul>
 * <li>tags: The health check tags to take into account
 * <li>format: html|json|jsonp
 * <li>includeDebug: If true, debug messages from result log are included.
 * <li>callback: For jsonp, the JS callback function name (defaults to "processHealthCheckResults")
 * <li>httpStatus: health check status to http status mapping in format httpStatus=WARN:418,CRITICAL:503,HEALTH_CHECK_ERROR:500.
 * </ul>
 *
 * For omitted health check status values the next best code will be used (e.g. for httpStatus=CRITICAL:503 a result WARN will
 * return 200, CRITICAL 503 and HEALTH_CHECK_ERROR also 503). By default all requests answer with an http status of 200.
 * <p>
 * Useful in combination with load balancers.
 * <p>
 * NOTE: This servlet registers directly (low-level) at the HttpService and is not processed by sling (better performance, fewer dependencies, no authentication required, 503 can be sent without the progress tracker information). */
@Component(label = "Apache Sling Health Check Executor Servlet",
        description = "Serializes health check results into html or json format",
        policy = ConfigurationPolicy.REQUIRE, metatype = true)
public class HealthCheckExecutorServlet extends HttpServlet {
    private static final long serialVersionUID = 8013511523994541848L;

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckExecutorServlet.class);
    public static final String PARAM_SPLIT_REGEX = "[,;]+";

    static class Param {
        final String name;
        final String description;
        Param(String n, String d) {
            name = n;
            description = d;
        }
    }

    static final Param PARAM_TAGS = new Param("tags",
            "Comma-separated list of health checks tags to select - can also be specified via path, e.g. /system/health/tag1,tag2.json. Exclusions can be done by prepending '-' to the tag name");
    static final Param PARAM_FORMAT = new Param("format", "Output format, html|json|jsonp|txt - an extension in the URL overrides this");
    static final Param PARAM_HTTP_STATUS = new Param("httpStatus", "Specify HTTP result code, for example"
            + " CRITICAL:503 (status 503 if result >= CRITICAL)"
            + " or CRITICAL:503,HEALTH_CHECK_ERROR:500,OK:418 for more specific HTTP status");

    static final Param PARAM_COMBINE_TAGS_WITH_OR = new Param("combineTagsWithOr", "Combine tags with OR, active by default. Set to false to combine with AND");
    static final Param PARAM_FORCE_INSTANT_EXECUTION = new Param("forceInstantExecution",
            "If true, forces instant execution by executing async health checks directly, circumventing the cache (2sec by default) of the HealthCheckExecutor");
    static final Param PARAM_OVERRIDE_GLOBAL_TIMEOUT = new Param("timeout",
            "(msec) a timeout status is returned for any health check still running after this period. Overrides the default HealthCheckExecutor timeout");

    static final Param PARAM_INCLUDE_DEBUG = new Param("hcDebug", "Include the DEBUG output of the Health Checks");

    static final Param PARAM_NAMES = new Param("names", "Comma-separated list of health check names to select. Exclusions can be done by prepending '-' to the health check name");

    static final String JSONP_CALLBACK_DEFAULT = "processHealthCheckResults";
    static final Param PARAM_JSONP_CALLBACK = new Param("callback", "name of the JSONP callback function to use, defaults to " + JSONP_CALLBACK_DEFAULT);

    static final Param [] PARAM_LIST = { PARAM_TAGS, PARAM_NAMES, PARAM_FORMAT, PARAM_HTTP_STATUS, PARAM_COMBINE_TAGS_WITH_OR,
        PARAM_FORCE_INSTANT_EXECUTION, PARAM_OVERRIDE_GLOBAL_TIMEOUT, PARAM_INCLUDE_DEBUG, PARAM_JSONP_CALLBACK};

    static final String FORMAT_HTML = "html";
    static final String FORMAT_JSON = "json";
    static final String FORMAT_JSONP = "jsonp";
    static final String FORMAT_TXT = "txt";
    static final String FORMAT_VERBOSE_TXT = "verbose.txt";

    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final String CONTENT_TYPE_TXT = "text/plain";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_JSONP = "application/javascript";
    private static final String STATUS_HEADER_NAME = "X-Health";

    private static final String CACHE_CONTROL_KEY = "Cache-control";
    private static final String CACHE_CONTROL_VALUE = "no-cache";

    private static final String SERVLET_PATH_DEFAULT = "/system/health";

    public static final String PROPERTY_SERVLET_PATH = "servletPath";
    @Property(name = PROPERTY_SERVLET_PATH, label = "Path",
            description = "Servlet path (defaults to " + SERVLET_PATH_DEFAULT + " in order to not be accessible via Apache/Internet)", value = SERVLET_PATH_DEFAULT)
    private String servletPath;

    private String[] servletPaths;

    public static final String PROPERTY_DISABLED = "disabled";
    @Property(name = PROPERTY_DISABLED, label = "Disabled",
            description = "Allows to disable the servlet if required for security reasons", boolValue = false)
    private boolean disabled;

    private static final String CORS_ORIGIN_HEADER_NAME = "Access-Control-Allow-Origin";
    public static final String CORS_ORIGIN_HEADER_DEFAULT_VALUE = "*";
    public static final String PROPERTY_CORS_ORIGIN_HEADER_VALUE = "cors.accessControlAllowOrigin";
    @Property(name = PROPERTY_CORS_ORIGIN_HEADER_VALUE, label = "CORS Access-Control-Allow-Origin", description = "Sets the Access-Control-Allow-Origin CORS header. If blank no header is sent.", value = CORS_ORIGIN_HEADER_DEFAULT_VALUE)
    private String corsAccessControlAllowOrigin;

    @Reference
    private HttpService httpService;

    @Reference
    HealthCheckExecutor healthCheckExecutor;

    @Reference
    ResultHtmlSerializer htmlSerializer;

    @Reference
    ResultJsonSerializer jsonSerializer;

    @Reference
    ResultTxtSerializer txtSerializer;

    @Reference
    ResultTxtVerboseSerializer verboseTxtSerializer;

    @Activate
    protected final void activate(final ComponentContext context) {
        final Dictionary<?, ?> properties = context.getProperties();
        this.servletPath = PropertiesUtil.toString(properties.get(PROPERTY_SERVLET_PATH), SERVLET_PATH_DEFAULT);
        this.disabled = PropertiesUtil.toBoolean(properties.get(PROPERTY_DISABLED), false);
        this.corsAccessControlAllowOrigin = PropertiesUtil.toString(properties.get(PROPERTY_CORS_ORIGIN_HEADER_VALUE), CORS_ORIGIN_HEADER_DEFAULT_VALUE);

        Map<String, HttpServlet> servletsToRegister = new LinkedHashMap<String, HttpServlet>();
        servletsToRegister.put(this.servletPath, this);
        servletsToRegister.put(this.servletPath + "." + FORMAT_HTML, new ProxyServlet(FORMAT_HTML));
        servletsToRegister.put(this.servletPath + "." + FORMAT_JSON, new ProxyServlet(FORMAT_JSON));
        servletsToRegister.put(this.servletPath + "." + FORMAT_JSONP, new ProxyServlet(FORMAT_JSONP));
        servletsToRegister.put(this.servletPath + "." + FORMAT_TXT, new ProxyServlet(FORMAT_TXT));
        servletsToRegister.put(this.servletPath + "." + FORMAT_VERBOSE_TXT, new ProxyServlet(FORMAT_VERBOSE_TXT));

        if (disabled) {
            LOG.info("Health Check Servlet is disabled by configuration");
            return;
        }

        for (final Map.Entry<String, HttpServlet> servlet : servletsToRegister.entrySet()) {
            try {
                LOG.debug("Registering {} to path {}", getClass().getSimpleName(), servlet.getKey());
                this.httpService.registerServlet(servlet.getKey(), servlet.getValue(), null, null);
            } catch (Exception e) {
                LOG.error("Could not register health check servlet: " + e, e);
            }
        }
        this.servletPaths = servletsToRegister.keySet().toArray(new String[0]);

    }

    @Deactivate
    public void deactivate(final ComponentContext componentContext) {
        if (disabled || this.servletPaths == null) {
            return;
        }

        for (final String servletPath : this.servletPaths) {
            try {
                LOG.debug("Unregistering path {}", servletPath);
                this.httpService.unregister(servletPath);
            } catch (Exception e) {
                LOG.error("Could not unregister health check servlet: " + e, e);
            }
        }
        this.servletPaths = null;
    }

    protected void doGet(final HttpServletRequest request, final HttpServletResponse response, final String format) throws ServletException, IOException {
        HealthCheckSelector selector = HealthCheckSelector.empty();
        String pathInfo = request.getPathInfo();
        String pathTokensStr = StringUtils.removeStart(splitFormat(pathInfo)[0], "/");

        List<String> tags = new ArrayList<String>();
        List<String> names = new ArrayList<String>();

        if (StringUtils.isNotBlank(pathTokensStr)) {
            String[] pathTokens = pathTokensStr.split(PARAM_SPLIT_REGEX);
            for (String pathToken : pathTokens) {
                if (pathToken.indexOf(' ') >= 0) {
                    // token contains space. assume it is a name
                    names.add(pathToken);
                } else {
                    tags.add(pathToken);
                }
            }
        }
        if (tags.size() == 0) {
            // if not provided via path use parameter or default
            tags = Arrays.asList(StringUtils.defaultIfEmpty(request.getParameter(PARAM_TAGS.name), "").split(PARAM_SPLIT_REGEX));
        }
        selector.withTags(tags.toArray(new String[0]));

        if (names.size() == 0) {
            // if not provided via path use parameter or default
            names = Arrays.asList(StringUtils.defaultIfEmpty(request.getParameter(PARAM_NAMES.name), "").split(PARAM_SPLIT_REGEX));
        }
        selector.withNames(names.toArray(new String[0]));

        final Boolean includeDebug = Boolean.valueOf(request.getParameter(PARAM_INCLUDE_DEBUG.name));
        final Map<Result.Status, Integer> statusMapping = request.getParameter(PARAM_HTTP_STATUS.name) != null ? getStatusMapping(request
                .getParameter(PARAM_HTTP_STATUS.name)) : null;

        HealthCheckExecutionOptions executionOptions = new HealthCheckExecutionOptions();
        executionOptions.setCombineTagsWithOr(Boolean.valueOf(StringUtils.defaultString(request.getParameter(PARAM_COMBINE_TAGS_WITH_OR.name), "true")));
        executionOptions.setForceInstantExecution(Boolean.valueOf(request.getParameter(PARAM_FORCE_INSTANT_EXECUTION.name)));
        String overrideGlobalTimeoutVal = request.getParameter(PARAM_OVERRIDE_GLOBAL_TIMEOUT.name);
        if (StringUtils.isNumeric(overrideGlobalTimeoutVal)) {
            executionOptions.setOverrideGlobalTimeout(Integer.valueOf(overrideGlobalTimeoutVal));
        }

        List<HealthCheckExecutionResult> executionResults = this.healthCheckExecutor.execute(selector, executionOptions);

        Result.Status mostSevereStatus = Result.Status.DEBUG;
        for (HealthCheckExecutionResult executionResult : executionResults) {
            Status status = executionResult.getHealthCheckResult().getStatus();
            if (status.ordinal() > mostSevereStatus.ordinal()) {
                mostSevereStatus = status;
            }
        }
        Result overallResult = new Result(mostSevereStatus, "Overall status " + mostSevereStatus);

        sendNoCacheHeaders(response);
        sendCorsHeaders(response);

        if (statusMapping != null) {
            Integer httpStatus = statusMapping.get(overallResult.getStatus());
            response.setStatus(httpStatus);
        }

        if (FORMAT_HTML.equals(format)) {
            sendHtmlResponse(overallResult, executionResults, request, response, includeDebug);
        } else if (FORMAT_JSON.equals(format)) {
            sendJsonResponse(overallResult, executionResults, null, response, includeDebug);
        } else if (FORMAT_JSONP.equals(format)) {
            String jsonpCallback = StringUtils.defaultIfEmpty(request.getParameter(PARAM_JSONP_CALLBACK.name), JSONP_CALLBACK_DEFAULT);
            sendJsonResponse(overallResult, executionResults, jsonpCallback, response, includeDebug);
        } else if (StringUtils.endsWith(format, FORMAT_TXT)) {
            sendTxtResponse(overallResult, response, StringUtils.equals(format, FORMAT_VERBOSE_TXT), executionResults, includeDebug);
        } else {
            response.setContentType("text/plain");
            response.getWriter().println("Invalid format " + format + " - supported formats: html|json|jsonp|txt|verbose.txt");
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        String format = splitFormat(pathInfo)[1];
        if (StringUtils.isBlank(format)) {
            // if not provided via extension use parameter or default
            format = StringUtils.defaultIfEmpty(request.getParameter(PARAM_FORMAT.name), FORMAT_HTML);
        }
        doGet(request, response, format);
    }

    private String[] splitFormat(String pathInfo) {
        for (String format : new String[] { FORMAT_HTML, FORMAT_JSON, FORMAT_JSONP, FORMAT_VERBOSE_TXT, FORMAT_TXT }) {
            String formatWithDot = "." + format;
            if (StringUtils.endsWith(pathInfo, formatWithDot)) {
                return new String[] { StringUtils.substringBeforeLast(pathInfo, formatWithDot), format };
            }
        }
        return new String[] { pathInfo, null };
    }

    private void sendTxtResponse(final Result overallResult, final HttpServletResponse response, boolean verbose,
            List<HealthCheckExecutionResult> executionResults, boolean includeDebug) throws IOException {
        response.setContentType(CONTENT_TYPE_TXT);
        response.setCharacterEncoding("UTF-8");
        if (verbose) {
            response.getWriter().write(verboseTxtSerializer.serialize(overallResult, executionResults, includeDebug));
        } else {
            response.getWriter().write(txtSerializer.serialize(overallResult));
        }
    }

    private void sendJsonResponse(final Result overallResult, final List<HealthCheckExecutionResult> executionResults, final String jsonpCallback,
            final HttpServletResponse response, boolean includeDebug)
            throws IOException {
        if (StringUtils.isNotBlank(jsonpCallback)) {
            response.setContentType(CONTENT_TYPE_JSONP);
        } else {
            response.setContentType(CONTENT_TYPE_JSON);
        }
        response.setCharacterEncoding("UTF-8");

        String resultJson = this.jsonSerializer.serialize(overallResult, executionResults, jsonpCallback, includeDebug);
        PrintWriter writer = response.getWriter();
        writer.append(resultJson);
    }

    private void sendHtmlResponse(final Result overallResult, final List<HealthCheckExecutionResult> executionResults,
            final HttpServletRequest request, final HttpServletResponse response, boolean includeDebug)
            throws IOException {
        response.setContentType(CONTENT_TYPE_HTML);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(STATUS_HEADER_NAME, overallResult.toString());
        response.getWriter().append(this.htmlSerializer.serialize(overallResult, executionResults, getHtmlHelpText(), includeDebug));
    }

    private void sendNoCacheHeaders(final HttpServletResponse response) {
        response.setHeader(CACHE_CONTROL_KEY, CACHE_CONTROL_VALUE);
    }

    private void sendCorsHeaders(final HttpServletResponse response) {
        if (StringUtils.isNotBlank(corsAccessControlAllowOrigin)) {
            response.setHeader(CORS_ORIGIN_HEADER_NAME, corsAccessControlAllowOrigin);
        }
    }

    private String getHtmlHelpText() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<h3>Supported URL parameters</h3>\n");
        for(Param p : PARAM_LIST) {
            sb.append("<b>").append(p.name).append("</b>:");
            sb.append(StringEscapeUtils.escapeHtml4(p.description));
            sb.append("<br/>");
        }
        return sb.toString();
    }

    Map<Result.Status, Integer> getStatusMapping(String mappingStr) throws ServletException {
        Map<Result.Status, Integer> statusMapping = new HashMap<Result.Status, Integer>();
        try {
            String[] bits = mappingStr.split("[,]");
            for (String bit : bits) {
                String[] tuple = bit.split("[:]");
                statusMapping.put(Result.Status.valueOf(tuple[0]), Integer.parseInt(tuple[1]));
            }
        } catch (Exception e) {
            throw new ServletException("Invalid parameter httpStatus=" + mappingStr + " " + e, e);
        }

        if (!statusMapping.containsKey(Result.Status.OK)) {
            statusMapping.put(Result.Status.OK, 200);
        }
        if (!statusMapping.containsKey(Result.Status.WARN)) {
            statusMapping.put(Result.Status.WARN, statusMapping.get(Result.Status.OK));
        }
        if (!statusMapping.containsKey(Result.Status.CRITICAL)) {
            statusMapping.put(Result.Status.CRITICAL, statusMapping.get(Result.Status.WARN));
        }
        if (!statusMapping.containsKey(Result.Status.HEALTH_CHECK_ERROR)) {
            statusMapping.put(Result.Status.HEALTH_CHECK_ERROR, statusMapping.get(Result.Status.CRITICAL));
        }
        return statusMapping;
    }

    private class ProxyServlet extends HttpServlet {

        private final String format;

        private ProxyServlet(final String format) {
            this.format = format;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            HealthCheckExecutorServlet.this.doGet(req, resp, format);
        }
    }


}
