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
package org.apache.sling.pipes;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.pipes.impl.CustomJsonWriter;
import org.apache.sling.pipes.impl.CustomWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Servlet executing plumber for a pipe path given as 'path' parameter,
 * it can also be launched against a container pipe resource directly (no need for path parameter)
 *
 */
@SlingServlet(resourceTypes = {Plumber.RESOURCE_TYPE,
        ContainerPipe.RESOURCE_TYPE,
        AuthorizablePipe.RESOURCE_TYPE,
        WritePipe.RESOURCE_TYPE,
        SlingQueryPipe.RESOURCE_TYPE},
        methods={"GET","POST"},
        extensions = {"json"})
public class PlumberServlet extends SlingAllMethodsServlet {
    Logger log = LoggerFactory.getLogger(this.getClass());

    protected static final String PARAM_PATH = "path";

    protected static final String PARAM_BINDINGS = "bindings";

    protected static final String PARAM_SIZE = "size";

    public static final int NB_MAX = 10;

    @Reference
    Plumber plumber;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        execute(request, response, false);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        execute(request, response, true);
    }

    protected void execute(SlingHttpServletRequest request, SlingHttpServletResponse response, boolean writeAllowed) throws ServletException {
        String path = request.getResource().getResourceType().equals(Plumber.RESOURCE_TYPE) ? request.getParameter(PARAM_PATH) : request.getResource().getPath();
        try {
            if (StringUtils.isBlank(path)) {
                throw new Exception("path should be provided");
            }
            String dryRun = request.getParameter(BasePipe.DRYRUN_KEY);
            int size = request.getParameter(PARAM_SIZE) != null ? Integer.parseInt(request.getParameter(PARAM_SIZE)) : NB_MAX;
            if (size < 0) {
                size = Integer.MAX_VALUE;
            }

            ResourceResolver resolver = request.getResourceResolver();
            Resource pipeResource = resolver.getResource(path);
            Pipe pipe = plumber.getPipe(pipeResource);
            PipeBindings bindings = pipe.getBindings();

            if (StringUtils.isNotBlank(dryRun) && dryRun.equals(Boolean.TRUE.toString())) {
                bindings.addBinding(BasePipe.DRYRUN_KEY, true);
            }

            String paramBindings = request.getParameter(PARAM_BINDINGS);
            if (StringUtils.isNotBlank(paramBindings)){
                try {
                    JSONObject bindingJSON = new JSONObject(paramBindings);
                    for (Iterator<String> keys = bindingJSON.keys(); keys.hasNext();){
                        String key = keys.next();
                        bindings.addBinding(key, bindingJSON.get(key));
                    }
                } catch (Exception e){
                    log.error("Unable to retrieve bindings information", e);
                }
            }
            if (!writeAllowed && pipe.modifiesContent()) {
                throw new Exception("This pipe modifies content, you should use a POST request");
            }
            OutputWriter writer = getWriter(request, response, pipe);
            int i = 0;
            Iterator<Resource> resourceIterator = pipe.getOutput();
            Set<String> paths = new HashSet<String>();
            while (resourceIterator.hasNext()){
                Resource resource = resourceIterator.next();
                paths.add(resource.getPath());
                if (++i < size) {
                    writer.writeItem(resource);
                }
            }
            writer.ends(i);
            plumber.persist(resolver, pipe, paths);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    OutputWriter getWriter(SlingHttpServletRequest request, SlingHttpServletResponse response, Pipe pipe) throws IOException, JSONException {
        OutputWriter[] candidates = new OutputWriter[]{new CustomJsonWriter(), new CustomWriter(), new DefaultOutputWriter()};
        for (OutputWriter candidate : candidates) {
            if (candidate.handleRequest(request)) {
                candidate.init(request, response, pipe);
                return candidate;
            }
        }
        return null;
    }
}