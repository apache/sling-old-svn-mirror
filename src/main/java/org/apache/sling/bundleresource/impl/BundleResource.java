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
package org.apache.sling.bundleresource.impl;

import static org.apache.jackrabbit.JcrConstants.NT_FILE;
import static org.apache.jackrabbit.JcrConstants.NT_FOLDER;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Resource that wraps a Bundle entry */
public class BundleResource extends AbstractResource {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ResourceResolver resourceResolver;

    private final BundleResourceCache bundle;

    private final MappedPath mappedPath;

    private final String path;

    private URL url;

    private final String resourceType;

    private final ResourceMetadata metadata;

    public static BundleResource getResource(ResourceResolver resourceResolver,
            BundleResourceCache bundle, MappedPath mappedPath,
            String resourcePath) {

        String entryPath = mappedPath.getEntryPath(resourcePath);

        // first try, whether the bundle has an entry with a trailing slash
        // which would be a folder. In this case we check whether the
        // repository contains an item with the same path. If so, we
        // don't create a BundleResource but instead return null to be
        // able to return an item-based resource
        URL entry = bundle.getEntry(entryPath.concat("/"));
        if (entry != null) {

            // append the slash to path for next steps
            resourcePath = resourcePath.concat("/");
        }

        // if there is no entry with a trailing slash, try plain name
        // which would then of course be a file
        if (entry == null) {
            entry = bundle.getEntry(entryPath);
        }

        // here we either have a folder for which no same-named item exists
        // or a bundle file
        if (entry != null) {
            return new BundleResource(resourceResolver, bundle, mappedPath,
                    resourcePath);
        }

        // the bundle does not contain the path
        return null;
    }

    public BundleResource(ResourceResolver resourceResolver,
            BundleResourceCache bundle, MappedPath mappedPath,
            String resourcePath) {

        this.resourceResolver = resourceResolver;
        this.bundle = bundle;
        this.mappedPath = mappedPath;

        metadata = new ResourceMetadata();
        metadata.setResolutionPath(resourcePath);
        metadata.setCreationTime(bundle.getBundle().getLastModified());
        metadata.setModificationTime(bundle.getBundle().getLastModified());

        if (resourcePath.endsWith("/")) {

            this.path = resourcePath.substring(0, resourcePath.length() - 1);
            this.resourceType = NT_FOLDER;
            metadata.put(ResourceMetadata.INTERNAL_CONTINUE_RESOLVING, Boolean.TRUE);

        } else {

            this.path = resourcePath;
            this.resourceType = NT_FILE;

            try {
                URL url = bundle.getEntry(mappedPath.getEntryPath(resourcePath));
                metadata.setContentLength(url.openConnection().getContentLength());
            } catch (Exception e) {
                // don't care, we just have no content length
            }
        }
    }

    public String getPath() {
        return path;
    }

    public String getResourceType() {
        return resourceType;
    }

    /** Returns <code>null</code>, bundle resources have no super type */
    public String getResourceSuperType() {
        return null;
    }

    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Type> Type adaptTo(Class<Type> type) {
        if (type == InputStream.class) {
            return (Type) getInputStream(); // unchecked cast
        } else if (type == URL.class) {
            return (Type) getURL(); // unchecked cast
        }

        // fall back to nothing
        return super.adaptTo(type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", type=" + getResourceType()
                + ", path=" + getPath();
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Returns a stream to the bundle entry if it is a file. Otherwise returns
     * <code>null</code>.
     */
    private InputStream getInputStream() {
        // implement this for files only
        if (isFile()) {
            try {
                URL url = getURL();
                if (url != null) {
                    return url.openStream();
                }
            } catch (IOException ioe) {
                log.error(
                        "getInputStream: Cannot get input stream for " + this, ioe);
            }
        }

        // otherwise there is no stream
        return null;
    }

    private URL getURL() {
        if (url == null) {
            try {
                url = new URL(BundleResourceURLStreamHandler.PROTOCOL, null,
                        -1, path, new BundleResourceURLStreamHandler(
                                bundle.getBundle(), mappedPath.getEntryPath(path)));
            } catch (MalformedURLException mue) {
                log.error("getURL: Cannot get URL for " + this, mue);
            }
        }

        return url;
    }

    @Override
    public Iterator<Resource> listChildren() {
        return new BundleResourceIterator(this);
    }

    BundleResourceCache getBundle() {
        return bundle;
    }

    MappedPath getMappedPath() {
        return mappedPath;
    }

    boolean isFile() {
        return NT_FILE.equals(getResourceType());
    }
}
