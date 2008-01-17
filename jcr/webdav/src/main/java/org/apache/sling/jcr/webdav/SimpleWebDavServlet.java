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
package org.apache.sling.jcr.webdav;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;

/**
 * The <code>SimpleWebDavServlet</code>
 * 
 * @scr.component
 */
public class SimpleWebDavServlet extends SimpleWebdavServlet {

    /** @scr.property value="/dav" */
    private static final String PROP_CONTEXT = "dav.root";

    /** @scr.property value="Sling WebDAV" */
    private static final String PROP_REALM = "dav.realm";

    private static final String DEFAULT_CONTEXT = "/dav";

    /** @scr.reference */
    private HttpService httpService;

    /** @scr.reference */
    private SlingRepository repository;

    // the alias under which the servlet is registered, null if not
    private String contextPath;

    @Override
    public Repository getRepository() {
        return repository;
    }
    
    // ---------- AbstractWebdavServlet overwrite ------------------------------

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        
        // redirect to the default workspace if directly addressing the servlet
        String pinfo = request.getPathInfo();
        if (pinfo == null || "/".equals(pinfo)) {
            String uri = request.getRequestURI();
            if (pinfo == null) {
                uri += "/";
            }
            uri += repository.getDefaultWorkspace();
            response.sendRedirect(uri);
        }
        
        super.service(request, response);
    }
    
    // ---------- SCR integration ----------------------------------------------

    protected void activate(ComponentContext componentContext) throws Exception {
        Dictionary<?, ?> props = componentContext.getProperties();

        String context = getString(props, PROP_CONTEXT, DEFAULT_CONTEXT);

        Dictionary<String, String> initparams = new Hashtable<String, String>();

        initparams.put(INIT_PARAM_RESOURCE_PATH_PREFIX, context);

        String value = getString(props, PROP_REALM, null);
        if (value != null) {
            initparams.put(INIT_PARAM_AUTHENTICATE_HEADER, "Basic Realm=\""
                + value + "\"");
        }
        
        // for now, the ResourceConfig is fixed
        final String configPath = "/webdav-resource-config.xml";
        final ResourceConfig rc = new ResourceConfig();
        final URL cfg = getClass().getResource(configPath);
        if(cfg == null) {
            throw new IOException("ResourceConfig source not found:" + configPath);
        }
        rc.parse(cfg);
        setResourceConfig(rc);

        // value = getString(props, INIT_PARAM_MISSING_AUTH_MAPPING, null);
        // if (value != null) {
        // initparams.put(INIT_PARAM_MISSING_AUTH_MAPPING, value);
        // }

          // Register servlet, and set the contextPath field to signal successful registration
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
