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
package org.apache.sling.muppet.sling.impl;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.muppet.api.MuppetFacade;
import org.apache.sling.muppet.api.RulesEngine;
import org.apache.sling.muppet.sling.api.JsonResultRenderer;
import org.apache.sling.muppet.sling.api.RulesResourceParser;

/** Sling Servlet that renders a Resource that contains Muppet rules definitions,
 *  after evaluating the rules.
 *  {@link RulesResourceParser} defines the resource format, and {@link JsonResultRenderer}
 *  defines the output format. 
 */
@SuppressWarnings("serial")
@SlingServlet(extensions="json",resourceTypes="sling/muppet.rules",methods="GET",selectors="muppet")
public class MuppetSlingServlet extends SlingSafeMethodsServlet {

    @Reference
    private MuppetFacade muppet;
    
    @Reference
    private RulesResourceParser parser;
    
    @Reference
    private JsonResultRenderer renderer;
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException,IOException {
        
        // TODO restrict execution to admin?
        
        // TODO we could cache the engine + rules, not sure if it's worth it...
        final RulesEngine engine = muppet.getNewRulesEngine();
        engine.addRules(parser.parseResource(request.getResource()));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        renderer.render(engine.evaluateRules(), response.getWriter());
        response.getWriter().flush();
    }
}
