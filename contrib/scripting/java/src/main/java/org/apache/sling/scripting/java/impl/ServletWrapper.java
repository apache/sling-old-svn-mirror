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

package org.apache.sling.scripting.java.impl;

import java.io.IOException;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.compiler.CompilationResult;
import org.apache.sling.commons.compiler.CompilerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class wraps the java servlet and handles its compilation,
 * instantiation, invocation und destruction.
 */
public class ServletWrapper {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** The servlet config. */
    private final ServletConfig config;

    /** Sling IO Provider. */
    private final SlingIOProvider ioProvider;

    /** The name of the generated class. */
    private final String className;

    /** The path to the servlet. */
    private final String sourcePath;

    private volatile long lastModificationTest = 0L;

    private volatile Class<?> servletClass;

    private volatile Servlet theServlet;

    private long available = 0L;

    private volatile Exception compileException;

    /**
     * A wrapper for servlets.
     */
    public ServletWrapper(final ServletConfig config,
                          final SlingIOProvider ioProvider,
                          final String servletPath) {
        this.config = config;
        this.ioProvider = ioProvider;
        this.sourcePath = servletPath;
        this.className = CompilerUtil.mapSourcePath(this.sourcePath).substring(1).replace('/', '.');
    }

    /**
     * Call the servlet.
     * @param request The current request.
     * @param response The current response.
     * @throws ServletException
     * @throws IOException
     */
    public void service(HttpServletRequest request,
                         HttpServletResponse response)
    throws Exception {
        try {
            if ((available > 0L) && (available < Long.MAX_VALUE)) {
                if (available > System.currentTimeMillis()) {
                    response.setDateHeader("Retry-After", available);
                    response.sendError
                        (HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                         "Servlet unavailable.");
                    logger.error("Java servlet {} is unavailable.", this.sourcePath);
                    return;
                }
                // Wait period has expired. Reset.
                available = 0;
            }

            // check for compilation
            if (this.lastModificationTest == 0 ) {
                synchronized (this) {
                    if (this.lastModificationTest == 0 ) {
                        try {
                            // clear exception
                            this.compileException = null;
                            this.compile();
                        } catch (Exception ex) {
                            // store exception for futher access attempts
                            this.compileException = ex;
                            throw ex;
                        } finally {
                            this.lastModificationTest = System.currentTimeMillis();
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
            logger.error("Java servlet {} is unavailable.", this.sourcePath);
            return;
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

    /** Handle the modification. */
    public void handleModification() {
        this.lastModificationTest = 0;
    }

    private void compile() throws Exception {
        final CompilationUnit unit = new CompilationUnit(this.sourcePath, className, ioProvider);
        final CompilationResult result = this.ioProvider.getCompiler().compile(new org.apache.sling.commons.compiler.CompilationUnit[] {unit},
                ioProvider.getOptions());

        final List<CompilerMessage> errors = result.getErrors();
        if ( errors != null && errors.size() > 0 ) {
            throw CompilerException.create(errors, this.sourcePath);
        }
        if ( result.didCompile() || this.theServlet == null ) {
            destroy();

            this.servletClass = result.loadCompiledClass(this.className);
            final Servlet servlet = (Servlet) servletClass.newInstance();
            servlet.init(config);

            theServlet = servlet;

        }
    }

    protected final static class CompilerException extends ServletException {

        private static final long serialVersionUID = 7353686069328527452L;

        public static CompilerException create(final List<CompilerMessage> errors,
                final String fileName) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("Compilation errors in ");
            buffer.append(fileName);
            buffer.append(":\n");
            for(final CompilerMessage e : errors) {
                buffer.append("Line ");
                buffer.append(e.getLine());
                buffer.append(", column ");
                buffer.append(e.getColumn());
                buffer.append(" : " );
                buffer.append(e.getMessage());
                buffer.append("\n");
            }
            return new CompilerException(buffer.toString());
        }

        public CompilerException(final String message) {
           super(message);
        }
    }
}
