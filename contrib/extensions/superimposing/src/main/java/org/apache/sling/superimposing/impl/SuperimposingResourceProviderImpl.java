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
package org.apache.sling.superimposing.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.superimposing.SuperimposingResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Superimposing resource provider.
 * Maps a single source path to the target root path, with or without overlay depending on configuration.
 */
public class SuperimposingResourceProviderImpl implements SuperimposingResourceProvider {

    private static final Logger log = LoggerFactory.getLogger(SuperimposingResourceProviderImpl.class);

    private final String rootPath;
    private final String rootPrefix;
    private final String sourcePath;
    private final String sourcePathPrefix;
    private final boolean overlayable;
    private final String toString;
    private ServiceRegistration registration;

    SuperimposingResourceProviderImpl(String rootPath, String sourcePath, boolean overlayable) {
        this.rootPath = rootPath;
        this.rootPrefix = rootPath.concat("/");
        this.sourcePath = sourcePath;
        this.sourcePathPrefix = sourcePath.concat("/");
        this.overlayable = overlayable;
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [path=").append(rootPath).append(", ");
        sb.append("sourcePath=").append(sourcePath).append(", ");
        sb.append("overlayable=").append(overlayable).append("]");
        this.toString = sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public Resource getResource(ResourceResolver resolver, HttpServletRequest httpServletRequest, String path) {
        return getResource(resolver, path);
    }

    /**
     * {@inheritDoc}
     */
    public Resource getResource(ResourceResolver resolver, String path) {
        final String mappedPath = mapPath(this, resolver, path);
        if (null != mappedPath) {
            // the existing resource where the superimposed content is retrieved from
            final Resource mappedResource = resolver.getResource(mappedPath);
            if (null != mappedResource) {
                return new SuperimposingResource(mappedResource, path);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Resource> listChildren(Resource resource) {

        // unwrap resource if it is a wrapped resource
        final Resource currentResource;
        if (resource instanceof ResourceWrapper) {
            currentResource = ((ResourceWrapper)resource).getResource();
        }
        else {
            currentResource = resource;
        }

        // delegate resource listing to resource resolver
        if (currentResource instanceof SuperimposingResource) {
            final SuperimposingResource res = (SuperimposingResource) currentResource;
            final ResourceResolver resolver = res.getResource().getResourceResolver();
            final Iterator<Resource> children = resolver.listChildren(res.getResource());
            return new SuperimposingResourceIterator(this, children);
        }
        return null;
    }

    /**
     * Maps a path below the superimposing root to the target resource's path.
     * @param provider Superimposing resource provider
     * @param resolver Resource resolver
     * @param path Path to map
     * @return Mapped path or null if no mapping available
     */
    static String mapPath(SuperimposingResourceProviderImpl provider, ResourceResolver resolver, String path) {
        if (provider.overlayable) {
            return mapPathWithOverlay(provider, resolver, path);
        }
        else {
            return mapPathWithoutOverlay(provider, resolver, path);
        }
    }

    /**
     * Maps a path below the superimposing root to the target resource's path with check for overlaying.
     * @param provider Superimposing resource provider
     * @param resolver Resource resolver
     * @param path Path to map
     * @return Mapped path or null if no mapping available
     */
    static String mapPathWithOverlay(SuperimposingResourceProviderImpl provider, ResourceResolver resolver, String path) {
        if (StringUtils.equals(path, provider.rootPath)) {
            // Superimposing root path cannot be overlayed
            return mapPathWithoutOverlay(provider, resolver, path);
        }
        else if (StringUtils.startsWith(path, provider.rootPrefix)) {
            if (hasOverlayResource(resolver, path)) {
                // overlay item exists, allow underlying resource provider to step in
                return null;
            }
            else {
                // overlay item does not exist, overlay cannot be applied, fallback to mapped path without overlay
                return mapPathWithoutOverlay(provider, resolver, path);
            }
        }
        return null;
    }

    static boolean hasOverlayResource(ResourceResolver resolver, String path) {
        // check for overlay resource by checking directly in underlying JCR
        final Session session = resolver.adaptTo(Session.class);
        try {
            return (null != session && session.itemExists(path));
        } catch (RepositoryException e) {
            log.error("Error accessing the repository. ", e);
        }
        return false;
    }

    /**
     * Maps a path below the superimposing root to the target resource's path without check for overlaying.
     * @param provider Superimposing resource provider
     * @param resolver Resource resolver
     * @param path Path to map
     * @return Mapped path or null if no mapping available
     */
    static String mapPathWithoutOverlay(SuperimposingResourceProviderImpl provider, ResourceResolver resolver, String path) {
        final String mappedPath;
        if (StringUtils.equals(path, provider.rootPath)) {
            mappedPath = provider.sourcePath;
        } else if (StringUtils.startsWith(path, provider.rootPrefix)) {
            mappedPath = StringUtils.replaceOnce(path, provider.rootPrefix, provider.sourcePathPrefix);
        } else {
            mappedPath = null;
        }
        return mappedPath;
    }

    /**
     * Maps a path below the target resource to the superimposed resource's path.
     *
     * @param provider
     * @param path
     * @return
     */
    static String reverseMapPath(SuperimposingResourceProviderImpl provider, String path) {
        final String mappedPath;
        if (path.startsWith(provider.sourcePathPrefix)) {
            mappedPath = StringUtils.replaceOnce(path, provider.sourcePathPrefix, provider.rootPrefix);
        } else if (path.equals(provider.sourcePath)) {
            mappedPath = provider.rootPath;
        } else {
            mappedPath = null;
        }
        return mappedPath;
    }

    //---------- Service Registration

    void registerService(BundleContext context) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "Provider of superimposed resources");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(ROOTS, new String[]{rootPath});

        registration = context.registerService(SERVICE_NAME, this, props);

        log.info("Registered {}", this);
    }

    void unregisterService() {
        if (registration != null) {
            registration.unregister();
            registration = null;
            log.info("Unregistered {}", this);
        }
    }

    /**
     * @return Root path (destination for superimposing)
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * @return Source path (source for superimposing)
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * @return Overlayable yes/no
     */
    public boolean isOverlayable() {
        return overlayable;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SuperimposingResourceProviderImpl) {
            final SuperimposingResourceProviderImpl srp = (SuperimposingResourceProviderImpl)o;
            return this.rootPath.equals(srp.rootPath) && this.sourcePath.equals(srp.sourcePath) && this.overlayable == srp.overlayable;

        }
        return false;
    }

    @Override
    public String toString() {
        return toString;
    }
}
