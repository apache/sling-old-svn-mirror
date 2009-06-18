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

import javax.jcr.Session;

import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.webdav.impl.helper.SlingLocatorFactory;
import org.apache.sling.jcr.webdav.impl.helper.SlingSessionProvider;

/**
 * The <code>SlingWebDavServlet</code> implements the WebDAV protocol as a
 * default servlet for Sling handling all WebDAV methods.
 * 
 * @scr.component
 *  immediate="true"
 *  metatype="no"
 *
 * @scr.service
 *  interface="javax.servlet.Servlet"
 *
 * @scr.property
 *  name="service.description"
 *  value="Sling WebDAV Servlet"
 *
 * @scr.property
 *  name="service.vendor"
 *  value="The Apache Software Foundation"
 *
 * Use this as the default servlet for all requests to Sling
 * @scr.property
 *  name="sling.servlet.resourceTypes"
 *  value="sling/servlet/default"
 * @scr.property
 *  name="sling.servlet.methods"
 *  value="*"
 */
public class SlingWebDavServlet extends AbstractSlingWebDavServlet {

    private DavLocatorFactory locatorFactory;
    
    private SessionProvider sessionProvider;

    //---------- SimpleWebdavServlet overwrites -------------------------------
    
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
}
