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
package org.apache.sling.hc.sling.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.hc.api.EvaluationResult;
import org.apache.sling.hc.api.HealthCheckFacade;
import org.apache.sling.hc.api.RulesEngine;
import org.apache.sling.hc.sling.api.RulesResourceParser;
import org.apache.sling.hc.util.TaggedRuleFilter;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Sling Servlet that renders a Resource that contains health check rules 
 *  definitions, after evaluating the rules.
 *  {@link RulesResourceParser} defines the resource format, and {@link JsonResultRenderer}
 *  defines the output format. 
 */
@SuppressWarnings("serial")
@SlingServlet(
        extensions={"json","html"},
        resourceTypes="sling/healthcheck/rules",
        methods="GET",
        selectors=SlingHealthCheckServlet.HC_SELECTOR)
public class SlingHealthCheckServlet extends SlingSafeMethodsServlet {

    public static final String HC_SELECTOR = "healthcheck";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private SlingHealthCheckWebconsolePlugin consolePlugin;
    
    @Reference
    private HealthCheckFacade healthcheck;
    
    @Reference
    private RulesResourceParser parser;
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    static interface Renderer {
        String getExtension();
        String getContentType();
        void render(List<EvaluationResult> results, Writer output, Map<String, String> options) throws IOException;
    }
    
    private final Renderer [] renderers = { new JsonResultRendererImpl(), new HtmlResultRendererImpl() };
    
    @Activate
    public void activate(ComponentContext ctx) {
        consolePlugin = new SlingHealthCheckWebconsolePlugin(ctx.getBundleContext(), resourceResolverFactory, this);
    }
    
    @Deactivate
    public void deactivate(ComponentContext ctx) {
        consolePlugin.deactivate();
        consolePlugin = null;
    }
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException {
        executeRules(request.getResource(), getRuleTagsFromRequest(request), request.getRequestPathInfo().getExtension(), response, null);
    }
    
    void executeRules(Resource rulesRoot, 
            String [] tags, 
            String extension, 
            HttpServletResponse response,
            Map<String, String> renderingOptions) 
            throws ServletException, IOException {
        final RulesEngine engine = healthcheck.getNewRulesEngine();
        engine.addRules(parser.parseResource(rulesRoot));
        final TaggedRuleFilter filter = new TaggedRuleFilter(tags);
        
        Renderer renderer = null;
        for(Renderer x : renderers) {
            if(extension.equals(x.getExtension())) {
                renderer = x;
                break;
            }
        }
        if(renderer == null) {
            throw new ServletException("No Renderer found for extension " + extension);
        }
        
        log.info("Executing rules found under {} with {} and rendering with {}", 
                new Object[] { rulesRoot.getPath(), filter, renderer });
        response.setContentType(renderer.getContentType());
        response.setCharacterEncoding("UTF-8");
        renderer.render(engine.evaluateRules(filter), response.getWriter(), renderingOptions);
        response.getWriter().flush();
    }
    
    static String [] getRuleTagsFromRequest(SlingHttpServletRequest r) {
        // Get request selectors and remove this servlet's selector
        final List<String> result = new ArrayList<String>();
        for(String tag : r.getRequestPathInfo().getSelectors()) {
            if(!HC_SELECTOR.equals(tag)) {
                result.add(tag);
            }
        }
        return result.toArray(new String[] {});
    }
}
