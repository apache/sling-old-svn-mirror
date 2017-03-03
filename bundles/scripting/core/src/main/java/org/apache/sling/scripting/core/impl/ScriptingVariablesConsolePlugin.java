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
import java.io.PrintWriter;
import java.net.URL;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;

/**
 * Web Console Plugin exposing all binding provider values.
 */
@Component
@Service
@Properties({
    @Property(name = WebConsoleConstants.PLUGIN_LABEL, value = ScriptingVariablesConsolePlugin.LABEL),
    @Property(name = WebConsoleConstants.PLUGIN_TITLE, value = ScriptingVariablesConsolePlugin.TITLE),
    @Property(name = "felix.webconsole.category", value = "Sling")
})
public class ScriptingVariablesConsolePlugin extends AbstractWebConsolePlugin {

    protected static final String LABEL = "scriptingvariables";
    protected static final String TITLE = "Scripting Variables";
    /**
     * 
     */
    private static final long serialVersionUID = 261709110347150295L;
    
    private static final String JS_RES_PATH = "scriptingvariables/ui/scriptingvariables.js";
    
    /**
     * The script engine manager.
     */
    @Reference
    private ScriptEngineManager scriptEngineManager;

    public ScriptingVariablesConsolePlugin() {
    }

    /**
     * Automatically called from 
     * <a href="https://github.com/apache/felix/blob/4a60744d0f88f351551e4cb4673eb60b8fbd21d3/webconsole/src/main/java/org/apache/felix/webconsole/AbstractWebConsolePlugin.java#L510">AbstractWebConsolePlugin#spoolResource</a>
     * 
     * @param path the requested path
     * @return either a URL from which to spool the resource requested through the given path or {@code null} 
     */
    public URL getResource(String path) {
        if (path.endsWith(JS_RES_PATH)) {
            return this.getClass().getResource("/" + JS_RES_PATH);
        }
        return null;
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final PrintWriter pw = response.getWriter();
        pw.append("<script type='text/javascript' src='").append(JS_RES_PATH).append("'></script>");
        pw.append("<div id='content'>");
        pw.append("<table class='content'  cellpadding='0' cellspacing='0' width='100%'>");
        pw.append("<tr><th colspan='3' class='content container'>Sling Scripting Variables</th></tr>");
        pw.append("<tr class='content'><td class='content' colspan='3'>Provide a resource path url and script engine (via extension) and then click on 'Retrieve Variables' to expose all script bindings variables for context 'request' which are available for that resource and script engine.</td></tr>"); 
        pw.append("<tr class='content'>");
        pw.append("<td class='content'>Resource Url (without selectors and extension)</td> ");
        pw.append("<td class='content' colspan='2'><input type ='text' name='form.path' placeholder='path' required='required' value='/' ");
        pw.append("class='input ui-state-default ui-corner-all inputText' size='50' pattern='^/{1}.*'></td></tr>");
        pw.append("<tr class='content'>");
        pw.append("<td class='content'>Script Engine</td> ");
        pw.append("<td class='content' colspan='2'><select name='form.extension'>");
        for (ScriptEngineFactory factory : scriptEngineManager.getEngineFactories()) {
            for (String extension : factory.getExtensions()) {
                pw.append("<option value='" + extension + "'>"+extension + " (" + factory.getEngineName() +")</option>");
            }
            pw.append("<option value=''>all (unfiltered)</option>");
        }
        pw.append("</select> ");
        pw.append("<button type='button' id='submitButton'> Retrieve Variables </button></td></tr></table>");
        pw.append("<div id='response'></div>");
    }
}
