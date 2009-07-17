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

    /** Reload flag. */
    private boolean reload = true;

    /** Compiler options. */
    private final Options options;

    private Servlet theServlet;
    private long available = 0L;
    private boolean firstTime = true;
    private ServletException compileException;
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
        this.options = options;
        this.ctxt = new CompilationContext(servletUri, options,
                ioProvider, servletCache, this);
    }

    /**
     * Set the reload flag.
     * @param reload
     */
    public void setReload(boolean reload) {
        this.reload = reload;
    }

    /**
     * Get the servlet.
     * @return The servlet.
     * @throws ServletException
     * @throws IOException
     * @throws FileNotFoundException
     */
    private Servlet getServlet()
    throws ServletException, IOException, FileNotFoundException {
        if (reload) {
            synchronized (this) {
                if (reload) {
                    destroy();

                    Servlet servlet = null;

                    try {
                        final Class<?> servletClass = ctxt.load();
                        servlet = (Servlet) servletClass.newInstance();
                    } catch (IllegalAccessException e) {
                        throw new ServletException(e);
                    } catch (InstantiationException e) {
                        throw new ServletException(e);
                    } catch (Exception e) {
                        throw new ServletException(e);
                    }

                    servlet.init(config);

                    theServlet = servlet;
                    reload = false;
                }
            }
        }
        return theServlet;
    }

    /**
     * Sets the compilation exception for this ServletWrapper.
     *
     * @param je The compilation exception
     */
    public void setCompilationException(ServletException je) {
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
    public void service(HttpServletRequest request,
                        HttpServletResponse response)
    throws ServletException, IOException, FileNotFoundException {

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
                    return;
                }
                // Wait period has expired. Reset.
                available = 0;
            }

            /*
             * (1) Compile
             */
            if (options.getDevelopment() || firstTime ) {
                synchronized (this) {
                    firstTime = false;

                    // The following sets reload to true, if necessary
                    ctxt.compile();
                }
            } else {
                if (compileException != null) {
                    // Throw cached compilation exception
                    throw compileException;
                }
            }

            /*
             * (2) (Re)load servlet class file
             */
            getServlet();

        } catch (FileNotFoundException ex) {
            ctxt.incrementRemoved();
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                  ex.getMessage());
            } catch (IllegalStateException ise) {
                logger.error("Java servlet source not found." +
                       ex.getMessage(), ex);
            }
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

            /*
             * (3) Service request
             */
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
