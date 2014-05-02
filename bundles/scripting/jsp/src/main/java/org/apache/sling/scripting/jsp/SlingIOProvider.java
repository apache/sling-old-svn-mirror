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
package org.apache.sling.scripting.jsp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.scripting.jsp.jasper.IOProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingIOProvider</code>
 */
class SlingIOProvider implements IOProvider {

    private static final String WEB_INF_TAGS = "/WEB-INF/tags";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(SlingIOProvider.class);

    private final ThreadLocal<ResourceResolver> requestResourceResolver;

    private final ClassLoaderWriter classLoaderWriter;

    private final JavaCompiler javaCompiler;

    SlingIOProvider(final ClassLoaderWriter classLoaderWriter, final JavaCompiler compiler) {
        this.requestResourceResolver = new ThreadLocal<ResourceResolver>();
        this.classLoaderWriter = classLoaderWriter;
        this.javaCompiler = compiler;
    }

    /**
     * Set the thread context resource resolver.
     */
    ResourceResolver setRequestResourceResolver(final ResourceResolver resolver) {
        final ResourceResolver old = requestResourceResolver.get();
        requestResourceResolver.set(resolver);
        return old;
    }

    /**
     * Reset the thread context resource resolver.
     */
    void resetRequestResourceResolver(final ResourceResolver resolver) {
        requestResourceResolver.set(resolver);
    }

    // ---------- IOProvider interface -----------------------------------------

    /**
     * Returns an InputStream for the file name which is looked up with the
     * ResourceProvider and retrieved from the Resource if the StreamProvider
     * interface is implemented.
     */
    public InputStream getInputStream(final String path)
            throws FileNotFoundException, IOException {
        if ( path.startsWith(":") ) {
            return this.classLoaderWriter.getInputStream(path.substring(1));
        }
        ResourceResolver resolver = requestResourceResolver.get();
        if (resolver != null) {
            try {
                final Resource resource = resolver.getResource(cleanPath(path, true));
                if (resource != null) {
                    final InputStream stream = resource.adaptTo(InputStream.class);
                    if (stream != null) {
                        return stream;
                    }
                }
            } catch (final SlingException se) {
                throw (IOException) new IOException(
                    "Failed to get InputStream for " + path).initCause(se);
            }
        }
        throw new FileNotFoundException("Cannot find " + path);
    }

    /**
     * Returns the value of the last modified meta data field of the resource
     * found at file name or zero if the meta data field is not set. If the
     * resource does not exist or an error occurrs finding the resource, -1 is
     * returned.
     */
    public long lastModified(final String path) {
        if ( path.startsWith(":") ) {
            return this.classLoaderWriter.getLastModified(path.substring(1));
        }
        ResourceResolver resolver = requestResourceResolver.get();
        if (resolver != null) {
            try {
                final Resource resource = resolver.getResource(cleanPath(path, true));
                if (resource != null) {
                    ResourceMetadata meta = resource.getResourceMetadata();
                    long modTime = meta.getModificationTime();
                    return (modTime > 0) ? modTime : 0;
                }
            } catch (final SlingException se) {
                log.error("Cannot get last modification time for " + path, se);
            }
        }
        // fallback to "non-existant" in case of problems
        return -1;
    }

    /**
     * Removes the named item from the repository.
     */
    public boolean delete(final String path) {
        return this.classLoaderWriter.delete(path.substring(1));
    }

    /**
     * Returns an output stream to write to the repository.
     */
    public OutputStream getOutputStream(final String path) {
        return this.classLoaderWriter.getOutputStream(path.substring(1));
    }

    /**
     * Renames a node in the repository.
     */
    public boolean rename(final String oldFileName, final String newFileName) {
        return this.classLoaderWriter.rename(oldFileName.substring(1), newFileName.substring(1));
    }

    /**
     * Creates a folder hierarchy in the repository.
     */
    public boolean mkdirs(final String path) {
        // we just do nothing
        return true;
    }

    /**
     * @see org.apache.sling.scripting.jsp.jasper.IOProvider#getClassLoader()
     */
    public ClassLoader getClassLoader() {
        return this.classLoaderWriter.getClassLoader();
    }

    // ---------- Helper Methods for JspServletContext -------------------------


    URL getURL(final String path) throws MalformedURLException {
        ResourceResolver resolver = requestResourceResolver.get();
        if (resolver != null) {
            try {
                final Resource resource = resolver.getResource(cleanPath(path, true));
                return (resource != null) ? resource.adaptTo(URL.class) : null;
            } catch (final SlingException se) {
                throw (MalformedURLException) new MalformedURLException(
                    "Cannot get URL for " + path).initCause(se);
            }
        }
        return null;
    }

    Set<String> getResourcePaths(final String path) {
        final Set<String> paths = new HashSet<String>();

        ResourceResolver resolver = requestResourceResolver.get();
        if (resolver != null) {
            try {
                final String cleanedPath = cleanPath(path, false);
                final boolean startsWithWebInfTags = cleanedPath.startsWith(WEB_INF_TAGS);

                Resource resource = resolver.getResource(startsWithWebInfTags ? cleanedPath.substring(WEB_INF_TAGS.length()) : cleanedPath);
                if (resource != null) {
                    Iterator<Resource> entries = resolver.listChildren(resource);
                    while (entries.hasNext()) {
                        final String entryPath = entries.next().getPath();
                        if (startsWithWebInfTags) {
                            paths.add(WEB_INF_TAGS + entryPath);
                        } else {
                            paths.add(entryPath);
                        }
                    }
                }
            } catch (final SlingException se) {
                log.warn("getResourcePaths: Cannot list children of " + path,
                    se);
            }
        }

        return paths.isEmpty() ? null : paths;
    }

    // ---------- internal -----------------------------------------------------

    private String cleanPath(String path, final boolean removeWebInfTags) {
        // replace backslash by slash
        path = path.replace('\\', '/');

        // cut off trailing slash
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (removeWebInfTags && path.startsWith(WEB_INF_TAGS)) {
            path = path.substring(WEB_INF_TAGS.length());
        }

        return path;
    }

    /**
     * @see org.apache.sling.scripting.jsp.jasper.IOProvider#getJavaCompiler()
     */
    public JavaCompiler getJavaCompiler() {
        return this.javaCompiler;
    }
}
