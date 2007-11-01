/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.microsling.scripting;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.scripting.SlingScript;

public class MicroslingScriptServlet implements Servlet {

    private final SlingScript script;

    public MicroslingScriptServlet(SlingScript script) {
        this.script = script;
    }

    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {
        MicroslingScriptResolver.evaluateScript(script,
            (SlingHttpServletRequest) req, (SlingHttpServletResponse) res);
    }

    public ServletConfig getServletConfig() {
        return null;
    }

    public String getServletInfo() {
        return "Servlet for script " + script.getScriptPath();
    }

    public void init(ServletConfig config) {
    }

    public void destroy() {
    }

}
