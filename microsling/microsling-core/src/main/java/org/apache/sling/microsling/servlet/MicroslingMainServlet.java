/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.microsling.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.Filter;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.HttpStatusCodeException;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.microsling.MicroslingSlingHttpServletRequest;
import org.apache.sling.microsling.MicroslingSlingHttpServletResponse;
import org.apache.sling.microsling.contenttype.ResponseContentTypeResolverFilter;
import org.apache.sling.microsling.scripting.MicroslingScriptResolver;
import org.apache.sling.microsling.services.MicroslingServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main microsling servlet: apply Filters to the request using our
 * MicroSlingFilterHelper, select and delegate to a SlingServlet to process the
 * request.
 */
public class MicroslingMainServlet extends GenericServlet {

    private static final long serialVersionUID = 1L;

    private MicroSlingFilterHelper filterChain;

    private MicroslingServiceLocator serviceLocator;

    private MicroslingServletResolver servletResolver;

    private MicroslingScriptResolver scriptResolver;

    private static final Logger log = LoggerFactory.getLogger(MicroslingMainServlet.class);

    @Override
    public void init() throws ServletException {
        super.init();

        // this must be first as services may register later
        initServiceLocator();

        initFilterChain();
        initServletResolver();
        initScriptResolver();
    }

    /** init our filter chain */
    protected void initFilterChain() throws ServletException {
        filterChain = new MicroSlingFilterHelper(this);
        addFilter(new ResponseContentTypeResolverFilter());
    }

    /** init our servlets */
    protected void initServletResolver() throws ServletException {
        servletResolver = new MicroslingServletResolver(getServletContext());
    }

    /** init our serviceLocator */
    protected void initServiceLocator() throws ServletException {
        serviceLocator = new MicroslingServiceLocator();
    }

    /** init our scriptResolver */
    protected void initScriptResolver() throws ServletException {
        scriptResolver = new MicroslingScriptResolver();
        serviceLocator.registerService(SlingScriptResolver.class,
            scriptResolver);
    }

    protected Repository getRepository() throws SlingException {
        // Access our Repository
        final String repoAttr = Repository.class.getName();
        Repository repository = (Repository) getServletContext().getAttribute(
            repoAttr);
        if (repository == null) {
            throw new SlingException(
                "Repository not available in ServletContext attribute "
                    + repoAttr);
        }

        return repository;
    }

    /** authenticate the request by creating a JCR Session for later use */
    protected Session authenticate(ServletRequest request)
            throws SlingException {
        // We should probably extract the user name and password (Credentials)
        // if you wish from the request. For now we just log in as an
        // admin user
        try {
            Credentials credentials = new SimpleCredentials("admin",
                "admin".toCharArray());
            return getRepository().login(credentials);
        } catch (RepositoryException re) {
            throw new SlingException("Repository.login() failed: "
                + re.getMessage(), re);
        }
    }

    /**
     * Execute our Filters via MicroSlingFilterHelper, which calls our doService
     * method after executing the filters
     */
    public void service(ServletRequest req, ServletResponse resp)
            throws ServletException, IOException {

        Session session = authenticate(req);

        MicroslingSlingHttpServletRequest request = new MicroslingSlingHttpServletRequest(
            (HttpServletRequest) req, session, serviceLocator);
        MicroslingSlingHttpServletResponse response = new MicroslingSlingHttpServletResponse(
            (HttpServletResponse) resp);

        // our filters might need the SlingRequestContext to store info in it
        filterChain.service(request, response);
    }

    @Override
    public void destroy() {
        // just for completeness, we have to take down our filters
        if (filterChain != null) {
            filterChain.destroy();
            filterChain = null;
        }

        // destroy registered servlets
        if (servletResolver != null) {
            servletResolver.destroy();
            servletResolver = null;
        }

        // destroy base class at the end
        super.destroy();
    }

    /**
     * Called by
     * {@link MicroSlingFilterHelper#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
     * after all filters have been processed.
     */
    void doService(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws SlingException, IOException {

        try {
            Servlet requestServlet = servletResolver.resolveServlet(request);

            if (requestServlet == null) {
                // TODO: decide whether this is 500 or rather 404
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No Servlet found to handle resource " + request.getResource());
            } else {
                requestServlet.service(request, response);
            }
        } catch (HttpStatusCodeException hts) {
            response.sendError(hts.getStatusCode(), hts.getMessage());
        } catch (IOException ioe) {
            throw ioe;
        } catch (SlingException se) {
            throw se;
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage() + "\n" + sw.toString());
        }
    }

    /** Add a filter to our MicroSlingFilterHelper */
    protected void addFilter(Filter filter) throws ServletException {
        filterChain.addFilter(filter);
    }

}
