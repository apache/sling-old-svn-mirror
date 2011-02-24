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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.commons.compiler.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingIOProvider</code>
 */
public class SlingIOProvider  {

    /** Default logger */
    private final Logger logger = LoggerFactory.getLogger(SlingIOProvider.class);

    private ThreadLocal<ResourceResolver> requestResourceResolver;

    private final JavaCompiler compiler;

    /** Options for compilation. */
    private final CompilerOptions options;

    /** Options for compilation including the force compile option. */
    private final CompilerOptions forceCompileOptions;

    /**
     * Servlet cache.
     */
    private final ServletCache servletCache = new ServletCache();

    /**
     * Constructor.
     */
    SlingIOProvider(final JavaCompiler compiler,
                    final CompilerOptions options) {
        this.requestResourceResolver = new ThreadLocal<ResourceResolver>();
        this.compiler = compiler;
        this.options = options;
        this.forceCompileOptions = CompilerOptions.copyOptions(options);
        this.forceCompileOptions.put(Options.KEY_FORCE_COMPILATION, true);
    }

    void destroy() {
        this.servletCache.destroy();
    }

    void setRequestResourceResolver(ResourceResolver resolver) {
        requestResourceResolver.set(resolver);
    }

    void resetRequestResourceResolver() {
        requestResourceResolver.remove();
    }

    // ---------- IOProvider interface -----------------------------------------

    public JavaCompiler getCompiler() {
        return this.compiler;
    }

    public CompilerOptions getOptions() {
        return this.options;
    }

    public CompilerOptions getForceCompileOptions() {
        return this.forceCompileOptions;
    }

    public ServletCache getServletCache() {
        return this.servletCache;
    }

    /**
     * Returns an InputStream for the file name which is looked up with the
     * ResourceProvider and retrieved from the Resource if the StreamProvider
     * interface is implemented.
     */
    public InputStream getInputStream(String fileName)
    throws FileNotFoundException, IOException {
        try {
            final Resource resource = getResourceInternal(fileName);
            if (resource == null) {
                throw new FileNotFoundException("Cannot find " + fileName);
            }

            final InputStream stream = resource.adaptTo(InputStream.class);
            if (stream == null) {
                throw new FileNotFoundException("Cannot find " + fileName);
            }

            return stream;

        } catch (SlingException se) {
            throw (IOException) new IOException(
                "Failed to get InputStream for " + fileName).initCause(se);
        }
    }

    /**
     * Returns the value of the last modified meta data field of the resource
     * found at file name or zero if the meta data field is not set. If the
     * resource does not exist or an error occurrs finding the resource, -1 is
     * returned.
     */
    public long lastModified(String fileName) {
        try {
            final Resource resource = getResourceInternal(fileName);
            if (resource != null) {
                final ResourceMetadata meta = resource.getResourceMetadata();
                final long modTime = meta.getModificationTime();
                return (modTime > 0) ? modTime : 0;
            }

        } catch (SlingException se) {
            logger.error("Cannot get last modification time for " + fileName, se);
        }

        // fallback to "non-existant" in case of problems
        return -1;
    }

    public URL getURL(String path) throws MalformedURLException {
        try {
            final Resource resource = getResourceInternal(path);
            return resource != null ? resource.adaptTo(URL.class) : null;
        } catch (SlingException se) {
            throw (MalformedURLException) new MalformedURLException(
                "Cannot get URL for " + path).initCause(se);
        }
    }

    public Set<String> getResourcePaths(final String path) {
        final Set<String> paths = new HashSet<String>();
        try {
            final Resource resource = getResourceInternal(path);
            if (resource != null) {
                final Iterator<Resource> entries = resource.getResourceResolver().listChildren(resource);
                while (entries.hasNext()) {
                    paths.add(entries.next().getPath());
                }
            }
        } catch (SlingException se) {
            logger.warn("Unable to get resource at path " + path, se);
        }

        return paths.isEmpty() ? null : paths;
    }

    private Resource getResourceInternal(String path) throws SlingException {
        ResourceResolver resolver = requestResourceResolver.get();
        if (resolver != null) {
            return resolver.getResource(path);
        }

        return null;
    }

}
