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

import java.net.URL;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.apache.sling.jcr.api.SlingRepository;

/**
 * The <code>SimpleWebDavServlet</code>
 * 
 * @scr.component
 *  immediate="true"
 *  metatype="no"
 */
abstract class AbstractSlingWebDavServlet extends SimpleWebdavServlet {

    /** @scr.reference */
    private SlingRepository repository;

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public void init() throws ServletException {
        super.init();

        // for now, the ResourceConfig is fixed
        final String configPath = "/webdav-resource-config.xml";
        final ResourceConfig rc = new ResourceConfig();
        final URL cfg = getClass().getResource(configPath);
        if (cfg == null) {
            throw new UnavailableException("ResourceConfig source not found:"
                + configPath);
        }
        
        rc.parse(cfg);
        setResourceConfig(rc);
    }

}
