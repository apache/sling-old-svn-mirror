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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoader;
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

    private final SlingScriptHelper scriptHelper;

    /**
     * The compiled and instantiated servlet. This field may be null in which case a new servlet
     * instance is created per request.
     */
    private volatile Servlet theServlet;

    /** Flag handling an unavailable exception. */
    private volatile long available = 0L;

    /** The exception thrown by the compilation. */
    private volatile Exception compileException;

    /**
     * A wrapper for servlets.
     */
    public ServletWrapper(final ServletConfig config,
                          final SlingIOProvider ioProvider,
                          final String servletPath,
                          final SlingScriptHelper scriptHelper) {
        this.config = config;
        this.ioProvider = ioProvider;
        this.sourcePath = servletPath;
        this.className = CompilerUtil.mapSourcePath(this.sourcePath).substring(1).replace('/', '.');
        this.scriptHelper = scriptHelper;
    }

    /**
     * Call the servlet.
     * @param request The current request.
     * @param response The current response.
     * @throws Exception
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

            final Servlet servlet = this.getServlet();

            // invoke the servlet
            if (servlet instanceof SingleThreadModel) {
                // sync on the wrapper so that the freshness
                // of the page is determined right before servicing
                synchronized (this) {
                    servlet.service(request, response);
                }
            } else {
                servlet.service(request, response);
            }

        } catch (final UnavailableException ex) {
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

    /**
     * Check if the used classloader is still valid
     */
    private boolean checkReload() {
        if ( theServlet != null && theServlet.getClass().getClassLoader() instanceof DynamicClassLoader ) {
            return !((DynamicClassLoader)theServlet.getClass().getClassLoader()).isLive();
        }
        return theServlet == null;
    }

    /**
     * Get the servlet class - if the used classloader is not valid anymore
     * the class is reloaded.
     */
    public Servlet getServlet()
    throws Exception {
        if ( this.compileException != null ) {
            throw this.compileException;
        }
        // check if the used class loader is still alive
        if (this.checkReload()) {
            synchronized (this) {
                if (this.checkReload()) {
                    logger.debug("Reloading {}", this.sourcePath);
                    this.compile();
                }
            }
        }

        return theServlet;
    }

    private void injectFields(final Servlet servlet) {
        for (Field field : servlet.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                try {
                    Type type = field.getGenericType();
                    if (type instanceof Class) {
                        Class<?> injectedClass = (Class<?>) type;
                        if (injectedClass.isInstance(scriptHelper)) {
                            field.set(servlet, scriptHelper);
                        } else if (injectedClass.isArray()) {
                            Object[] services = scriptHelper.getServices(injectedClass.getComponentType(), null);
                            Object arr = Array.newInstance(injectedClass.getComponentType(), services.length);
                            for (int i = 0; i < services.length; i++) {
                                Array.set(arr, i, services[i]);
                            }
                            field.set(servlet, arr);
                        } else {
                            field.set(servlet, scriptHelper.getService(injectedClass));
                        }
                    } else if (type instanceof ParameterizedType) {
                        ParameterizedType ptype = (ParameterizedType) type;
                        if (ptype.getActualTypeArguments().length != 1) {
                            logger.warn("Field {} of {} has more than one type parameter.", field.getName(), sourcePath);
                            continue;
                        }
                        Class<?> collectionType = (Class<?>) ptype.getRawType();
                        if (!(collectionType.equals(Collection.class) ||
                                collectionType.equals(List.class))) {
                            logger.warn("Field {} of {} was not an injectable collection type.", field.getName(), sourcePath);
                            continue;
                        }

                        Class<?> serviceType = (Class<?>) ptype.getActualTypeArguments()[0];
                        Object[] services = scriptHelper.getServices(serviceType, null);
                        field.set(servlet, Arrays.asList(services));
                    } else {
                        logger.warn("Field {} of {} was not an injectable type.", field.getName(), sourcePath);
                    }
                } catch (final IllegalArgumentException e) {
                    logger.error(String.format("Unable to inject into field %s of %s.", field.getName(), sourcePath), e);
                } catch (final IllegalAccessException e) {
                    logger.error(String.format("Unable to inject into field %s of %s.", field.getName(), sourcePath), e);
                } finally {
                    field.setAccessible(false);
                }
            }
        }
    }

    /**
     * Compile the servlet java class. If the compiled class has
     * injected fields, don't create an instance of it.
     */
    private void compile()
    throws Exception {
        logger.debug("Compiling {}", this.sourcePath);
        // clear exception
        this.compileException = null;
        try {
            final CompilerOptions opts = this.ioProvider.getForceCompileOptions();
            final CompilationUnit unit = new CompilationUnit(this.sourcePath, className, ioProvider);
            final CompilationResult result = this.ioProvider.getCompiler().compile(new org.apache.sling.commons.compiler.CompilationUnit[] {unit},
                    opts);

            final List<CompilerMessage> errors = result.getErrors();
            this.destroy();
            if ( errors != null && errors.size() > 0 ) {
                throw CompilerException.create(errors, this.sourcePath);
            }

            final Servlet servlet = (Servlet) result.loadCompiledClass(this.className).newInstance();

            servlet.init(this.config);
            this.injectFields(servlet);

            this.theServlet = servlet;

        } catch (final Exception ex) {
            // store exception for futher access attempts
            this.compileException = ex;
            throw ex;
        }
    }

    /** Compiler exception .*/
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
