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

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.webdav.impl.helper.SlingLocatorFactory;
import org.apache.sling.jcr.webdav.impl.helper.SlingResourceConfig;
import org.apache.sling.jcr.webdav.impl.helper.SlingSessionProvider;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * The <code>SlingWebDavServlet</code> implements the WebDAV protocol as a
 * default servlet for Sling handling all WebDAV methods.
 * 
 * @scr.component name="org.apache.sling.jcr.webdav.impl.servlets.SimpleWebDavServlet"
 *                label="%dav.name" description="%dav.description"
 *                immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value="Sling WebDAV Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="sling.servlet.resourceTypes"
 *               value="sling/servlet/default" private="true"
 * @scr.property name="sling.servlet.methods" value="*" private="true"
 */
public class SlingWebDavServlet extends SimpleWebdavServlet {

    /** @scr.property valueRef="DEFAULT_CONTEXT" */
    public static final String PROP_CONTEXT = "dav.root";

    /** @scr.property valueRef="DEFAULT_REALM" */
    public static final String PROP_REALM = "dav.realm";

    /** @scr.property valueRef="COLLECTION_TYPES_DEFAULT" */
    public static final String COLLECTION_TYPES = "collection.types";

    /** @scr.property valueRef="FILTER_PREFIXES_DEFAULT" */
    public static final String FILTER_PREFIXES = "filter.prefixes";

    /** @scr.property valueRef="EMPTY_DEFAULT" */
    public static final String FILTER_TYPES = "filter.types";

    /** @scr.property valueRef="EMPTY_DEFAULT" */
    public static final String FILTER_URIS = "filter.uris";

    /** @scr.property valueRef="TYPE_COLLECTIONS_DEFAULT" */
    public static final String TYPE_COLLECTIONS = "type.collections";

    /** @scr.property valueRef="TYPE_NONCOLLECTIONS_DEFAULT" */
    public static final String TYPE_NONCOLLECTIONS = "type.noncollections";

    /** @scr.property valueRef="TYPE_CONTENT_DEFAULT" */
    public static final String TYPE_CONTENT = "type.content";

    public static final String DEFAULT_CONTEXT = "/dav";

    public static final String DEFAULT_REALM = "Sling WebDAV";

    public static final String[] EMPTY_DEFAULT = new String[0];

    public static final String[] FILTER_PREFIXES_DEFAULT = new String[] {
        "rep", "jcr" };

    public static final String TYPE_COLLECTIONS_DEFAULT = "sling:Folder";

    public static final String TYPE_NONCOLLECTIONS_DEFAULT = "nt:file";

    public static final String TYPE_CONTENT_DEFAULT = "nt:resource";

    public static final String[] COLLECTION_TYPES_DEFAULT = new String[] {
        TYPE_NONCOLLECTIONS_DEFAULT, TYPE_CONTENT_DEFAULT };

    /** @scr.reference */
    private SlingRepository repository;

    /** @scr.reference */
    private HttpService httpService;

    /** @scr.reference */
    private ServletContext servletContext;

    private SlingResourceConfig resourceConfig;

    private DavLocatorFactory locatorFactory;

    private SessionProvider sessionProvider;
    
    private boolean simpleWebDavServletRegistered;

    // ---------- SimpleWebdavServlet overwrites -------------------------------

    @Override
    public void init() throws ServletException {
        super.init();
        
        setResourceConfig(resourceConfig);
    }
    
    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public DavLocatorFactory getLocatorFactory() {
        if (locatorFactory == null) {

            // configured default workspace name
            SlingRepository slingRepo = (SlingRepository) getRepository();
            String workspace = slingRepo.getDefaultWorkspace();

            // no configuration, try to login and acquire the default name
            if (workspace == null || workspace.length() == 0) {
                Session tmp = null;
                try {
                    tmp = slingRepo.login();
                    workspace = tmp.getWorkspace().getName();
                } catch (Throwable t) {
                    // TODO: log !!
                    workspace = "default"; // fall back name
                } finally {
                    if (tmp != null) {
                        tmp.logout();
                    }
                }
            }

            locatorFactory = new SlingLocatorFactory(workspace);
        }
        return locatorFactory;
    }

    @Override
    public synchronized SessionProvider getSessionProvider() {
        if (sessionProvider == null) {
            sessionProvider = new SlingSessionProvider();
        }
        return sessionProvider;
    }

    // ---------- SCR integration

    protected void activate(ComponentContext context)
            throws NamespaceException, ServletException {

        resourceConfig = new SlingResourceConfig(servletContext,
            context.getProperties());

        // Register servlet, and set the contextPath field to signal successful
        // registration
        Servlet simpleServlet = new SimpleWebDavServlet(resourceConfig,
            getRepository());
        httpService.registerServlet(resourceConfig.getServletContextPath(),
            simpleServlet, resourceConfig.getServletInitParams(), null);
        simpleWebDavServletRegistered = true;
    }

    protected void deactivate(ComponentContext context) {
        
        if (simpleWebDavServletRegistered) {
            httpService.unregister(resourceConfig.getServletContextPath());
            simpleWebDavServletRegistered = false;
        }

        resourceConfig = null;
    }
}
