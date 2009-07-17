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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingIOProvider</code>
 */
public class SlingIOProvider  {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(SlingIOProvider.class);

    private ThreadLocal<ResourceResolver> requestResourceResolver;

    private final ClassLoaderWriter classLoaderWriter;

    SlingIOProvider(final ClassLoaderWriter classLoaderWriter) {
        this.requestResourceResolver = new ThreadLocal<ResourceResolver>();
        this.classLoaderWriter = classLoaderWriter;
    }

    void setRequestResourceResolver(ResourceResolver resolver) {
        requestResourceResolver.set(resolver);
    }

    void resetRequestResourceResolver() {
        requestResourceResolver.remove();
    }

    // ---------- IOProvider interface -----------------------------------------

    /**
     * Returns an InputStream for the file name which is looked up with the
     * ResourceProvider and retrieved from the Resource if the StreamProvider
     * interface is implemented.
     */
    public InputStream getInputStream(String fileName)
    throws FileNotFoundException, IOException {
        if ( fileName.startsWith(":") ) {
            return this.classLoaderWriter.getInputStream(fileName.substring(1));
        }
        try {

            Resource resource = getResourceInternal(fileName);
            if (resource == null) {
                throw new FileNotFoundException("Cannot find " + fileName);
            }

            InputStream stream = resource.adaptTo(InputStream.class);
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
        if ( fileName.startsWith(":") ) {
            return this.classLoaderWriter.getLastModified(fileName.substring(1));
        }
        try {
            Resource resource = getResourceInternal(fileName);
            if (resource != null) {
                ResourceMetadata meta = resource.getResourceMetadata();
                long modTime = meta.getModificationTime();
                return (modTime > 0) ? modTime : 0;
            }

        } catch (SlingException se) {
            log.error("Cannot get last modification time for " + fileName, se);
        }

        // fallback to "non-existant" in case of problems
        return -1;
    }

    /**
     * Returns an output stream to write to the repository.
     */
    public OutputStream getOutputStream(String fileName) {
        return this.classLoaderWriter.getOutputStream(fileName.substring(1));
    }

    /* package */URL getURL(String path) throws MalformedURLException {
        try {
            Resource resource = getResourceInternal(path);
            return (resource != null) ? resource.adaptTo(URL.class) : null;
        } catch (SlingException se) {
            throw (MalformedURLException) new MalformedURLException(
                "Cannot get URL for " + path).initCause(se);
        }
    }

    /* package */Set<String> getResourcePaths(String path) {
        Set<String> paths = new HashSet<String>();

        ResourceResolver resolver = requestResourceResolver.get();
        if (resolver != null) {
            try {
                Resource resource = resolver.getResource(cleanPath(path));
                if (resource != null) {
                    Iterator<Resource> entries = resolver.listChildren(resource);
                    while (entries.hasNext()) {
                        paths.add(entries.next().getPath());
                    }
                }
            } catch (SlingException se) {
                log.warn("getResourcePaths: Cannot list children of " + path,
                    se);
            }
        }

        return paths.isEmpty() ? null : paths;
    }

    private Resource getResourceInternal(String path) throws SlingException {
        ResourceResolver resolver = requestResourceResolver.get();
        if (resolver != null) {
            return resolver.getResource(cleanPath(path));
        }

        return null;
    }

    // ---------- internal -----------------------------------------------------

    private String cleanPath(String path) {
        // replace backslash by slash
        path = path.replace('\\', '/');

        // cut off trailing slash
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }
}
