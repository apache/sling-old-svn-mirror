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

import java.util.List;

import javax.servlet.ServletException;

import org.apache.sling.scripting.java.jdt.EclipseJavaCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class CompilationContext {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /** The name of the generated class. */
    private final String className;

    /** The path to the servlet. */
    private final String sourcePath;

    /** The mapped path. */
    private final String mappedSourcePath;

    /** Compilation options. */
    private final Options options;

    /** The compiler instance. */
    private final EclipseJavaCompiler compiler;

    /** Sling IO Provider. */
    private final SlingIOProvider ioProvider;

    private ServletCache servletCache;

    private volatile long lastModificationTest = 0L;
    private int removed = 0;

    private Class<?> servletClass;

    private final ServletWrapper wrapper;

    /**
     * A new compilation context.
     * @param sourcePath The path to the servlet source.
     * @param options The compiler options
     * @param provider The Sling IO Provider
     * @param servletCache
     */
    public CompilationContext(final String sourcePath,
                              final Options options,
                              final SlingIOProvider provider,
                              ServletCache servletCache,
                              final ServletWrapper wrapper) {
        this.sourcePath = sourcePath;
        this.mappedSourcePath = CompilerUtil.mapSourcePath(this.sourcePath);
        this.className = CompilerUtil.makeClassPath(this.mappedSourcePath);

        this.options = options;
        this.ioProvider = provider;
        this.compiler = new EclipseJavaCompiler(this);

        this.servletCache = servletCache;
        this.wrapper = wrapper;
    }

    /**
     * Options
     */
    public Options getCompilerOptions() {
        return options;
    }

    /**
     * Provider
     */
    public SlingIOProvider getIOProvider() {
        return this.ioProvider;
    }

    /**
     * Return the path to the java servlet source file
     * @return The source file path.
     */
    public String getSourcePath() {
        return this.sourcePath;
    }

    public String getJavaClassName() {
        return this.mappedSourcePath.replace('/', '.');
    }

    /**
     * Return the path to the generated class file.
     * @return The class file path.
     */
    public String getClassFilePath() {
        return this.className;
    }

    public void incrementRemoved() {
        if (removed == 0 && servletCache != null) {
            servletCache.removeWrapper(sourcePath);
        }
        removed++;
    }

    public boolean isRemoved() {
        if (removed > 1 ) {
            return true;
        }
        return false;
    }

    public long getLastModificationTest() {
        return this.lastModificationTest;
    }

    public void clearLastModificationTest() {
        this.lastModificationTest = 0;
    }

    /**
     * Check if the compiled class file is older than the source file
     */
    public boolean isOutDated() {
        if ( this.lastModificationTest > 0 ) {
            return false;
        }
        final long sourceLastModified = this.ioProvider.lastModified(getSourcePath());

        final long targetLastModified = this.ioProvider.lastModified(getCompleteClassPath());
        if (targetLastModified < 0) {
            return true;
        }

        if (targetLastModified < sourceLastModified) {
            if (logger.isDebugEnabled()) {
                logger.debug("Compiler: outdated: " + getClassFilePath() + " "
                        + targetLastModified);
            }
            return true;
        }

        return false;

    }

    private String getCompleteClassPath() {
        return options.getDestinationPath() + getClassFilePath() + ".class";
    }

    // ==================== Compile and reload ====================

    public boolean compile() throws Exception {
        if (this.isOutDated()) {
            try {
                final List<CompilerError> errors = this.compiler.compile();
                if ( errors != null ) {
                    throw CompilerException.create(errors);
                }
                this.wrapper.setCompilationException(null);
                return true;
            } catch (Exception ex) {
                // Cache compilation exception
                this.wrapper.setCompilationException(ex);
                throw ex;
            } finally {
                this.lastModificationTest = System.currentTimeMillis();
            }
        }
        this.lastModificationTest = System.currentTimeMillis();
        return false;
    }

    /**
     * Load the class.
     */
    public Class<?> load()
    throws ServletException {
        final String name = this.getClassFilePath().substring(1).replace('/', '.');
        try {
            servletClass = this.options.getClassLoader().loadClass(name);
        } catch (ClassNotFoundException cex) {
            throw new ServletException("Servlet class not found: " + name, cex);
        } catch (Exception ex) {
            throw new ServletException("Unable to compile servlet: " + name, ex);
        }
        removed = 0;
        return servletClass;
    }

    protected final static class CompilerException extends ServletException {

        public static CompilerException create(List<CompilerError> errors) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("Compilation errors:\n");
            for(final CompilerError e : errors) {
                buffer.append(e.getFile());
                buffer.append(", line ");
                buffer.append(e.getStartLine());
                buffer.append(", column ");
                buffer.append(e.getStartColumn());
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
