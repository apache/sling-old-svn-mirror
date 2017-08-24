/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.pipes.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.pipes.BasePipe;
import org.apache.sling.pipes.ContainerPipe;
import org.apache.sling.pipes.OutputWriter;
import org.apache.sling.pipes.Plumber;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet executing plumber for a pipe path given as 'path' parameter,
 * it can also be launched against a container pipe resource directly (no need for path parameter)
 */
@Component(service = {Servlet.class},
        property= {
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + Plumber.RESOURCE_TYPE,
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ContainerPipe.RESOURCE_TYPE,
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + AuthorizablePipe.RESOURCE_TYPE,
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + WritePipe.RESOURCE_TYPE,
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + SlingQueryPipe.RESOURCE_TYPE,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json"
        })
public class PlumberServlet extends SlingAllMethodsServlet {
    Logger log = LoggerFactory.getLogger(this.getClass());

    protected static final String PARAM_PATH = "path";

    protected static final String PARAM_BINDINGS = "bindings";

    protected static final String PARAM_ASYNC = "async";

    @Reference
    Plumber plumber;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        if (Arrays.asList(request.getRequestPathInfo().getSelectors()).contains(BasePipe.PN_STATUS)){
            response.getWriter().append(plumber.getStatus(request.getResource()));
        } else {
            execute(request, response, false);
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        execute(request, response, true);
    }

    /**
     * Execution of a pipe corresponding to a request
     * @param request original request
     * @param response given response
     * @param writeAllowed should we consider this execution is about to modify content
     * @throws ServletException in case something is wrong...
     */
    protected void execute(SlingHttpServletRequest request, SlingHttpServletResponse response, boolean writeAllowed) throws ServletException {
        String path = request.getResource().getResourceType().equals(Plumber.RESOURCE_TYPE) ? request.getParameter(PARAM_PATH) : request.getResource().getPath();
        try {
            if (StringUtils.isBlank(path)) {
                throw new Exception("path should be provided");
            }
            Map bindings = getBindingsFromRequest(request, writeAllowed);
            String asyncParam = request.getParameter(PARAM_ASYNC);
            if (StringUtils.isNotBlank(asyncParam) && asyncParam.equals(Boolean.TRUE.toString())){
                Job job = plumber.executeAsync(request.getResourceResolver(), path, bindings);
                if (job != null){
                    response.getWriter().append("pipe execution registered as " + job.getId());
                    response.setStatus(HttpServletResponse.SC_CREATED);
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Some issue with your request, or server not being ready for async execution");
                }
            } else {
                OutputWriter writer = getWriter(request, response);
                plumber.execute(request.getResourceResolver(), path, bindings, writer, true);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Converts request into pipe bindings
     * @param request from where to extract bindings
     * @param writeAllowed should we consider this execution is about to modify content
     * @return map of bindings
     */
    protected Map getBindingsFromRequest(SlingHttpServletRequest request, boolean writeAllowed){
        Map bindings = new HashMap<>();
        String dryRun = request.getParameter(BasePipe.DRYRUN_KEY);
        if (StringUtils.isNotBlank(dryRun) && dryRun.equals(Boolean.TRUE.toString())) {
            bindings.put(BasePipe.DRYRUN_KEY, true);
        }
        String paramBindings = request.getParameter(PARAM_BINDINGS);
        if (StringUtils.isNotBlank(paramBindings)){
            try {
                bindings.putAll(JsonUtil.unbox(JsonUtil.parseObject(paramBindings)));
            } catch (Exception e){
                log.error("Unable to retrieve bindings information", e);
            }
        }
        bindings.put(BasePipe.READ_ONLY, !writeAllowed);
        return bindings;
    }

    /**
     * Retrieve an output writer depending on the request
     * @param request original request against which writers will be tested
     * @param response response writers will point to
     * @return instance of the created writer
     * @throws IOException bad handling of I/O streams,
     * @throws JsonException bad handling of json output
     */
    OutputWriter getWriter(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, JsonException {
        OutputWriter[] candidates = new OutputWriter[]{new CustomJsonWriter(), new CustomWriter(), new DefaultOutputWriter()};
        for (OutputWriter candidate : candidates) {
            if (candidate.handleRequest(request)) {
                candidate.init(request, response);
                return candidate;
            }
        }
        return null;
    }
}