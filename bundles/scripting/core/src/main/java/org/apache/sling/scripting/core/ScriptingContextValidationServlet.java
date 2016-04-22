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
package org.apache.sling.scripting.core;

import java.io.IOException;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.scripting.core.impl.helper.SlingScriptEngineManager;

@SlingServlet(
        resourceTypes = "sling/servlet/default",
        selectors = "bvpvars",
        methods = "GET"
)
public class ScriptingContextValidationServlet extends SlingSafeMethodsServlet implements OptingServlet {

    @Reference
    private SlingScriptEngineManager scriptEngineManager;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException,
            IOException {
        response.sendError(400, "No scripting context available.");
    }

    @Override
    public boolean accepts(SlingHttpServletRequest request) {
        String path = request.getPathInfo();
        String ext = path.substring(path.lastIndexOf('.') + 1);
        return scriptEngineManager.getEngineByExtension(ext) == null;
    }

}
