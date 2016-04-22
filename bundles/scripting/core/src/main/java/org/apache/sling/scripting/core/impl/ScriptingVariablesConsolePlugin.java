/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.core.impl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.osgi.framework.Constants;

@Component
@Service
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Script Cache"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = WebConsoleConstants.PLUGIN_LABEL, value = "scriptingvariables"),
    @Property(name = WebConsoleConstants.PLUGIN_TITLE, value = "Scripting Variables"),
    @Property(name = "felix.webconsole.category", value = "Status")
})
public class ScriptingVariablesConsolePlugin extends AbstractWebConsolePlugin {

    private static final String JS_RES_PATH = "scriptingvariables/ui/scriptingvariables.js";

    public ScriptingVariablesConsolePlugin() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getRequestURI().endsWith(JS_RES_PATH)) {
            response.setContentType("application/javascript");
            IOUtils.copy(getClass().getResourceAsStream("/" + JS_RES_PATH), response.getOutputStream());
        } else {
            super.doGet(request, response);
        }
    }

    @Override
    protected void renderContent(HttpServletRequest hsr, HttpServletResponse hsr1) throws ServletException,
            IOException {

        StringBuilder sb = new StringBuilder();
        sb.append("<script type='text/javascript' src='").append(JS_RES_PATH).append("'></script>");
        sb.append("<div id='cached-scripts' class='ui-widget statline'>");
        sb.append("<table class='nicetable ui-widget-content'> <tr> <td>");
        sb.append("<label for='form.path'> Provide a path to check available scripting variables </label></td> ");
        sb.append("<td> <input type ='text' name='form.path' placeholder='path' required='required' ");
        sb.append("class='input ui-state-default ui-corner-all inputText' size='50' pattern='^/{1}.*'> ");
        sb.append("<button type='button' id='submitButton'> Check </button></td></tr></table>");
        sb.append("<div id='response'></div>");

        hsr1.getWriter().write(sb.toString());
    }

    @Override
    public String getLabel() {
        return "scriptingvariables";
    }

    @Override
    public String getTitle() {
        return "Scripting Variables";
    }

}
