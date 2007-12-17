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
package org.apache.sling.core.impl.servlets;

import static java.lang.Boolean.TRUE;
import static org.apache.sling.api.scripting.SlingBindings.FLUSH;
import static org.apache.sling.api.scripting.SlingBindings.REQUEST;
import static org.apache.sling.api.scripting.SlingBindings.RESPONSE;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingScriptServlet</code> is a servlet encapsulating a
 * <code>SlingScript</code> instance for unified use in the
 * {@link SlingServletResolver}.
 */
public class SlingScriptServlet implements Servlet {

    private ServletConfig servletConfig;
    
    /**
     * The script to call in the
     * {@link #service(ServletRequest, ServletResponse)} method.
     */
    private final SlingScript script;

    public SlingScriptServlet(SlingScript script) {
        this.script = script;
    }

    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {

        SlingHttpServletRequest request = (SlingHttpServletRequest) req;

        try {
            // prepare the properties for the script
            SlingBindings props = new SlingBindings();
            props.put(REQUEST, req);
            props.put(RESPONSE, res);
            props.put(FLUSH, TRUE);

            res.setCharacterEncoding("UTF-8");
            res.setContentType(request.getResponseContentType());

            // evaluate the script now using the ScriptEngine
            script.eval(props);

        } catch (IOException ioe) {
            throw ioe;
        } catch (ServletException se) {
            throw se;
        } catch (Exception e) {
            throw new SlingException("Cannot get DefaultSlingScript: "
                + e.getMessage(), e);
        }
    }

    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public String getServletInfo() {
        return "Servlet for script " + script.getScriptResource().getURI();
    }

    public void init(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
    }

    public void destroy() {
    }

}
