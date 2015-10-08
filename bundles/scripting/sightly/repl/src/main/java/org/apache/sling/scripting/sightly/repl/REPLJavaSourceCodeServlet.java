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
package org.apache.sling.scripting.sightly.repl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SlingServlet(
        resourceTypes = {"repl/components/repl"},
        selectors = {"java"},
        methods = "GET",
        extensions = "html"
)
public class REPLJavaSourceCodeServlet extends SlingSafeMethodsServlet {

    private static final String FS_CLASSLOADER_SN = "org.apache.sling.commons.fsclassloader";
    private static final Logger LOGGER = LoggerFactory.getLogger(REPLJavaSourceCodeServlet.class);

    private File classesFolder;

    @Activate
    @SuppressWarnings("unused")
    protected void activate(ComponentContext componentContext) {
        for (Bundle bundle : componentContext.getBundleContext().getBundles()) {
            if (FS_CLASSLOADER_SN.equals(bundle.getSymbolicName())) {
                BundleContext context = bundle.getBundleContext();
                classesFolder = new File(context.getDataFile(""), "classes");
            }
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String sourceCode = getClassSourceCode();
        if (sourceCode.length() == 0) {
            StringBuilder configurationLink = new StringBuilder();
            configurationLink.append(request.getScheme()).append("://").append(request.getServerName());
            if (request.getServerPort() != 80) {
                configurationLink.append(":").append(request.getServerPort());
            }
            configurationLink.append(request.getContextPath()).append("/system/console/configMgr/org.apache.sling.scripting.sightly.impl.engine.SightlyEngineConfiguration");
            response.getWriter().write("/**\n * Please enable development mode at\n * " + configurationLink.toString() + "\n */");
        } else {
            response.getWriter().write(getClassSourceCode());
        }
    }

    private String getClassSourceCode() {
        if (classesFolder != null && classesFolder.isDirectory()) {
            File classFile = new File(classesFolder, "/apps/repl/components/repl/SightlyJava_template.java");
            if (classFile.isFile()) {
                try {
                    return IOUtils.toString(new FileInputStream(classFile), "UTF-8");
                } catch (IOException e) {
                    LOGGER.error("Unable to read file " + classFile.getAbsolutePath(), e);
                }
            }
        }
        LOGGER.warn("Source code for " + (classesFolder.isDirectory() ? classesFolder.getAbsolutePath() : "") +
                "/apps/repl/components/repl/SightlyJava_template.java was not found. Maybe you need to enable dev mode for Sightly?");
        return "";
    }

}
