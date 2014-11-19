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
package org.apache.sling.scripting.sightly.engine.extension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.script.Bindings;
import javax.servlet.Servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.scripting.sightly.api.ExtensionInstance;
import org.apache.sling.scripting.sightly.api.RenderContext;
import org.apache.sling.scripting.sightly.api.RuntimeExtension;
import org.apache.sling.scripting.sightly.api.RuntimeExtensionComponent;
import org.apache.sling.scripting.sightly.api.SightlyEngineException;
import org.apache.sling.scripting.sightly.api.SightlyRenderException;
import org.apache.sling.scripting.sightly.plugin.IncludePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(RuntimeExtension.class)
@Properties({
        @Property(name = RuntimeExtensionComponent.SCR_PROP_NAME, value = IncludePlugin.FUNCTION)
})
@SuppressWarnings("unused")
/**
 * Runtime support for including resources in a Sightly script through {@code data-sly-include}. For more details check the implementation
 * of the {@link org.apache.sling.scripting.sightly.plugin.IncludePlugin}.
 */
public class IncludeRuntimeExtension extends RuntimeExtensionComponent {

    private static final Logger LOG = LoggerFactory.getLogger(IncludeRuntimeExtension.class);

    private static final String OPTION_FILE = "file";
    private static final String OPTION_PREPEND_PATH = "prependPath";
    private static final String OPTION_APPEND_PATH = "appendPath";


    @Override
    public ExtensionInstance provide(final RenderContext renderContext) {

        return new ExtensionInstance() {

            private final Bindings bindings = renderContext.getBindings();

            @Override
            public Object call(Object... arguments) {
                checkArgumentCount(arguments, 2);
                String originalPath = renderContext.getObjectModel().coerceToString(arguments[0]);
                Map options = (Map) arguments[1];
                String path = buildPath(originalPath, options);
                if (path == null) {
                    throw new SightlyRenderException("Path for include is empty");
                }
                StringWriter output = new StringWriter();
                includeScript(path, new PrintWriter(output));
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

            private void includeScript(String script, PrintWriter out) {
                if (StringUtils.isEmpty(script)) {
                    LOG.error("Script path cannot be empty");
                } else {
                    SlingScriptHelper slingScriptHelper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
                    ServletResolver servletResolver = slingScriptHelper.getService(ServletResolver.class);
                    if (servletResolver != null) {
                        SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
                        Servlet servlet = servletResolver.resolveServlet(request.getResource(), script);
                        if (servlet != null) {
                            try {
                                SlingHttpServletResponse response = (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE);
                                PrintWriterResponseWrapper resWrapper = new PrintWriterResponseWrapper(out, response);
                                servlet.service(request, resWrapper);
                            } catch (Exception e) {
                            	throw new SightlyEngineException("failed to include script ".concat(script), e);
                            }
                        } else {
                            LOG.error("Failed to locate script {}", script);
                        }
                    } else {
                        LOG.error("Sling ServletResolver service is unavailable, failed to include {}", script);
                    }
                }
            }
        };
    }
}
