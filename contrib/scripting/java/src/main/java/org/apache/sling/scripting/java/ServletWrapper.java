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

package org.apache.sling.scripting.java;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingIOException;
import org.apache.sling.api.SlingServletException;
import org.apache.sling.api.scripting.SlingBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**

 */

public class ServletWrapper {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String servletUri;

    /** The servlet config. */
    private ServletConfig config;

    private Servlet theServlet;
    private long available = 0L;
    private Exception compileException;
    private CompilationContext ctxt;

    /**
     * A wrapper for servlets.
     */
    public ServletWrapper(final ServletConfig config,
                          final Options options,
                          final SlingIOProvider ioProvider,
                          final String servletPath,
                          final ServletCache servletCache) {
        this.config = config;
        this.servletUri = servletPath;
        this.ctxt = new CompilationContext(servletUri, options,
                ioProvider, servletCache, this);
    }

    public CompilationContext getCompilationContext() {
        return this.ctxt;
    }

    /**
     * Get the servlet.
     * @throws ServletException
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void getServlet()
    throws ServletException {
        destroy();

        Servlet servlet = null;

        try {
            final Class<?> servletClass = ctxt.load();
            servlet = (Servlet) servletClass.newInstance();
        } catch (IllegalAccessException e) {
            throw new ServletException(e);
        } catch (InstantiationException e) {
            throw new ServletException(e);
        } catch (ServletException se) {
            throw se;
        } catch (Exception e) {
            throw new ServletException(e);
        }

        servlet.init(config);

        theServlet = servlet;
    }

    /**
     * Sets the compilation exception for this ServletWrapper.
     *
     * @param je The compilation exception
     */
    public void setCompilationException(final Exception je) {
        this.compileException = je;
    }

    /**
     * @param bindings
     * @throws SlingIOException
     * @throws SlingServletException
     * @throws IllegalArgumentException if the Jasper Precompile controller
     *             request parameter has an illegal value.
     */
    public void service(SlingBindings bindings) {
        final SlingHttpServletRequest request = bindings.getRequest();
        final Object oldValue = request.getAttribute(SlingBindings.class.getName());
        try {
            request.setAttribute(SlingBindings.class.getName(), bindings);
            service(request, bindings.getResponse());
        } catch (SlingException se) {
            // rethrow as is
            throw se;
        } catch (IOException ioe) {
            throw new SlingIOException(ioe);
        } catch (ServletException se) {
            throw new SlingServletException(se);
        } finally {
            request.setAttribute(SlingBindings.class.getName(), oldValue);
        }
    }

    /**
     * Call the servlet.
     * @param request The current request.
     * @param response The current response.
     * @throws ServletException
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void service(HttpServletRequest request,
                        HttpServletResponse response)
    throws ServletException, IOException {
        try {

            if (ctxt.isRemoved()) {
                throw new FileNotFoundException(servletUri);
            }

            if ((available > 0L) && (available < Long.MAX_VALUE)) {
                if (available > System.currentTimeMillis()) {
                    response.setDateHeader("Retry-After", available);
                    response.sendError
                        (HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                         "Servlet unavailable.");
                    logger.error("Java servlet {} is unavailable.", this.servletUri);
                    return;
                }
                // Wait period has expired. Reset.
                available = 0;
            }

            // check for compilation
            if (ctxt.getLastModificationTest() == 0 ) {
                synchronized (this) {
                    if (ctxt.getLastModificationTest() == 0 ) {
                        if ( ctxt.compile() ) {
                            // (re)load the servlet class
                            getServlet();
                        }

                    } else if (compileException != null) {
                        // Throw cached compilation exception
                        throw compileException;
                    }
                }
            } else if (compileException != null) {
                // Throw cached compilation exception
                throw compileException;
            }

        } catch (FileNotFoundException ex) {
            ctxt.incrementRemoved();
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                  ex.getMessage());
                logger.error("Java servlet {} not found.", this.servletUri);
            } catch (IllegalStateException ise) {
                logger.error("Java servlet source not found." +
                       ex.getMessage(), ex);
            }
            return;
        } catch (SlingException ex) {
            throw ex;
        } catch (ServletException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServletException(ex);
        }

        try {

            // invoke the servlet
            if (theServlet instanceof SingleThreadModel) {
               // sync on the wrapper so that the freshness
               // of the page is determined right before servicing
               synchronized (this) {
                   theServlet.service(request, response);
                }
            } else {
                theServlet.service(request, response);
            }

        } catch (UnavailableException ex) {
            int unavailableSeconds = ex.getUnavailableSeconds();
            if (unavailableSeconds <= 0) {
                unavailableSeconds = 60;        // Arbitrary default
            }
            available = System.currentTimeMillis() +
                (unavailableSeconds * 1000L);
            response.sendError
                (HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                 ex.getMessage());
            logger.error("Java servlet {} is unavailable.", this.servletUri);
            return;
        } catch (SlingException ex) {
            throw ex;
        } catch (ServletException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    /**
     * Destroy the servlet.
     */
    public void destroy() {
        if (theServlet != null) {
            theServlet.destroy();
            theServlet = null;
        }
    }
}
