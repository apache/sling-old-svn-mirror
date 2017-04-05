/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine.extension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import javax.script.Bindings;
import javax.servlet.Servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.utils.BindingsUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.render.RuntimeObjectModel;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime support for including resources in a HTL script through {@code data-sly-include}.
 */
@Component(
        service = RuntimeExtension.class,
        property = {
                RuntimeExtension.NAME + "=" + RuntimeFunction.INCLUDE
        }
)
public class IncludeRuntimeExtension implements RuntimeExtension {

    private static final Logger LOG = LoggerFactory.getLogger(IncludeRuntimeExtension.class);
    private static final String OPTION_FILE = "file";
    private static final String OPTION_PREPEND_PATH = "prependPath";
    private static final String OPTION_APPEND_PATH = "appendPath";
    private static final String OPTION_REQUEST_ATTRIBUTES = "requestAttributes";


    @Override
    public Object call(final RenderContext renderContext, Object... arguments) {
        ExtensionUtils.checkArgumentCount(RuntimeFunction.INCLUDE, arguments, 2);
        RuntimeObjectModel runtimeObjectModel = renderContext.getObjectModel();
        String originalPath = runtimeObjectModel.toString(arguments[0]);
        Map options = (Map) arguments[1];
        String path = buildPath(originalPath, options);
        StringWriter output = new StringWriter();
        final Bindings bindings = renderContext.getBindings();
        SlingHttpServletRequest request = BindingsUtils.getRequest(bindings);
        Map originalAttributes = ExtensionUtils.setRequestAttributes(request, (Map)options.remove(OPTION_REQUEST_ATTRIBUTES));
        includeScript(bindings, path, new PrintWriter(output));
        ExtensionUtils.setRequestAttributes(request, originalAttributes);
        return output.toString();

    }

    private String buildPath(String path, Map options) {
        if (StringUtils.isEmpty(path)) {
            path = (String) options.get(OPTION_FILE);
        }
        if (StringUtils.isEmpty(path)) {
            return null;
        }
        String prependPath = (String) options.get(OPTION_PREPEND_PATH);
        String appendPath = (String) options.get(OPTION_APPEND_PATH);
        if (StringUtils.isNotEmpty(prependPath)) {
            path = prependPath + path;
        }
        if (StringUtils.isNotEmpty(appendPath)) {
            path = path + appendPath;
        }
        return path;
    }

    private void includeScript(final Bindings bindings, String script, PrintWriter out) {
        if (StringUtils.isEmpty(script)) {
            throw new SightlyException("Path for data-sly-include is empty");
        } else {
            LOG.debug("Attempting to include script {}.", script);
            SlingScriptHelper slingScriptHelper = BindingsUtils.getHelper(bindings);
            ServletResolver servletResolver = slingScriptHelper.getService(ServletResolver.class);
            if (servletResolver != null) {
                SlingHttpServletRequest request = BindingsUtils.getRequest(bindings);
                Servlet servlet = servletResolver.resolveServlet(request.getResource(), script);
                if (servlet != null) {
                    try {
                        SlingHttpServletResponse response = BindingsUtils.getResponse(bindings);
                        PrintWriterResponseWrapper resWrapper = new PrintWriterResponseWrapper(out, response);
                        servlet.service(request, resWrapper);
                    } catch (Exception e) {
                        throw new SightlyException("Failed to include script " + script, e);
                    }
                } else {
                    throw new SightlyException("Failed to locate script " + script);
                }
            } else {
                throw new SightlyException("Sling ServletResolver service is unavailable, failed to include " + script);
            }
        }
    }
}
