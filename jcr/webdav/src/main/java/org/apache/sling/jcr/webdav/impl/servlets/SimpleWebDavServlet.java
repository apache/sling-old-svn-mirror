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
package org.apache.sling.jcr.webdav.impl.servlets;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * The <code>SimpleWebDavServlet</code>
 * 
 * @scr.component label="%dav.name" description="%dav.description"
 * @scr.property name="service.description"
 *                value="Sling JcrResourceResolverFactory Implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 */
public class SimpleWebDavServlet extends AbstractSlingWebDavServlet {

    /** @scr.property valueRef="DEFAULT_CONTEXT" */
    private static final String PROP_CONTEXT = "dav.root";

    /** @scr.property valueRef="DEFAULT_REALM" */
    private static final String PROP_REALM = "dav.realm";

    private static final String DEFAULT_CONTEXT = "/dav";

    private static final String DEFAULT_REALM = "Sling WebDAV";
    
    /** @scr.reference */
    private HttpService httpService;

    // the alias under which the servlet is registered, null if not
    private String contextPath;

    // ---------- AbstractWebdavServlet overwrite ------------------------------

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        final String pinfo = request.getPathInfo();

        if (pinfo == null || "/".equals(pinfo)) {
            // redirect to the default workspace if directly addressing the
            // servlet
            // and if the default workspace name is not null (in which case we'd
            // need
            // to login to find out the actual workspace name, SLING-256)
            SlingRepository slingRepo = (SlingRepository) getRepository();
            if (slingRepo.getDefaultWorkspace() == null) {
                response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    "JCR workspace name required, please add it to the end of the URL"
                        + " (for the Jackrabbit embedded repository the default name is 'default') ");
            } else {
                String uri = request.getRequestURI();
                if (pinfo == null) {
                    uri += "/";
                }
                uri += slingRepo.getDefaultWorkspace();
                response.sendRedirect(uri);
            }
        }

        super.service(request, response);
    }

    // ---------- SCR integration ----------------------------------------------

    protected void activate(ComponentContext componentContext)
            throws NamespaceException, ServletException {

        Dictionary<?, ?> props = componentContext.getProperties();

        String context = getString(props, PROP_CONTEXT, DEFAULT_CONTEXT);

        Dictionary<String, String> initparams = new Hashtable<String, String>();

        initparams.put(INIT_PARAM_RESOURCE_PATH_PREFIX, context);

        String value = getString(props, PROP_REALM, DEFAULT_REALM);
        initparams.put(INIT_PARAM_AUTHENTICATE_HEADER, "Basic Realm=\"" + value
            + "\"");

        // Register servlet, and set the contextPath field to signal successful
        // registration
        httpService.registerServlet(context, this, initparams, null);
        this.contextPath = context;
    }

    protected void deactivate(ComponentContext context) {
        if (contextPath != null) {
            httpService.unregister(contextPath);
            contextPath = null;
        }
    }

    // ---------- internal -----------------------------------------------------

    private String getString(Dictionary<?, ?> props, String name,
            String defaultValue) {
        Object propValue = props.get(name);
        return (propValue instanceof String)
                ? (String) propValue
                : defaultValue;
    }
}
