/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/

package org.apache.sling.scripting.core.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.scripting.api.ScriptCache;
import org.osgi.framework.Constants;

@Component
@Service
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Script Cache"),
        @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
        @Property(name = WebConsoleConstants.PLUGIN_LABEL, value = ScriptCacheConsolePlugin.CONSOLE_LABEL),
        @Property(name = WebConsoleConstants.PLUGIN_TITLE, value = ScriptCacheConsolePlugin.CONSOLE_TITLE),
        @Property(name = "felix.webconsole.category", value = "Sling")
})
public class ScriptCacheConsolePlugin extends AbstractWebConsolePlugin {

    public static final String CONSOLE_LABEL = "scriptcache";
    public static final String CONSOLE_TITLE = "Script Cache Status";
    public static final String RESOURCES = CONSOLE_LABEL + "/ui";

    private static final String SCRIPTCACHE_JS = "scriptcache.js";
    private static final String CTYPE_JAVASCRIPT = "application/javascript";
    private static final String POST_SCRIPT = "script";

    @Reference
    private ScriptCache scriptCache = null;

    public ScriptCacheConsolePlugin() {
        super();
    }

    @Override
    public String getTitle() {
        return CONSOLE_TITLE;
    }

    @Override
    public String getLabel() {
        return CONSOLE_LABEL;
    }

    @Override
    protected void renderContent(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        if (scriptCache instanceof ScriptCacheImpl) {
            ScriptCacheImpl scriptCacheImpl = (ScriptCacheImpl) scriptCache;
            List<String> scripts = new ArrayList<String>(scriptCacheImpl.getCachedScripts());
            StringBuilder sb = new StringBuilder();
            sb.append("<script type='text/javascript' src='").append(RESOURCES).append("/").append(SCRIPTCACHE_JS).append("'></script>");
            sb.append("<div id='cached-scripts' class='ui-widget statline'>");
            if (scripts.size() > 0) {
                Collections.sort(scripts);
                sb.append("<p class='ui-widget-header'>Cached Scripts</p>");
                sb.append("<table class='nicetable ui-widget-content'>");
                int i = 0;
                for (String script : scripts) {
                    sb.append("<tr class='").append(i % 2 == 0 ? "even" : "odd").append(" ui-state-default'><td>").append(++i).append
                            ("<td><code>").append(script).append("</code></td><td><button type='button' " +
                            "data-script='").append(script).append("'>Remove</button></td></tr>");
                }
                sb.append("<tr><td colspan='3'><button type='button' id='clearcache'>Clear Cache</button></td></tr>");
                sb.append("</table>");

            } else {
                sb.append("<p class='ui-state-highlight'>The Script Cache doesn't contain any scripts.</p>");
            }
            sb.append("</div>");
            httpServletResponse.getWriter().write(sb.toString());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getRequestURI().endsWith(RESOURCES + "/" + SCRIPTCACHE_JS)) {
            response.setContentType(CTYPE_JAVASCRIPT);
            IOUtils.copy(getClass().getResourceAsStream("/" + RESOURCES + "/" + SCRIPTCACHE_JS), response.getOutputStream());
        } else {
            super.doGet(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String script = req.getParameter(POST_SCRIPT);
        if (StringUtils.isNotEmpty(script)) {
            if ("all".equals(script)) {
                scriptCache.clear();
                renderContent(req, resp);
            } else {
                boolean success = scriptCache.removeScript(script);
                if (success) {
                    renderContent(req, resp);
                }
            }
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }
}


