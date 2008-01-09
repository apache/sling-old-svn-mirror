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
package org.apache.sling.microsling.scripting;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;

public class MicroslingScriptServlet implements Servlet {

    private final SlingScript script;

    public MicroslingScriptServlet(SlingScript script) {
        this.script = script;
    }

    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {

        SlingBindings bindings = new SlingBindings();
        bindings.put(SlingBindings.REQUEST, req);
        bindings.put(SlingBindings.RESPONSE, res);
        script.eval(bindings);
    }

    public ServletConfig getServletConfig() {
        return null;
    }

    public String getServletInfo() {
        return "Servlet for script " + script.getScriptResource().getPath();
    }

    public void init(ServletConfig config) {
    }

    public void destroy() {
    }

}
