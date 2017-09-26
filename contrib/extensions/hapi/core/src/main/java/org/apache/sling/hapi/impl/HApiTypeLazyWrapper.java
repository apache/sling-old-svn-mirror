/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.hapi.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.hapi.HApiException;
import org.apache.sling.hapi.HApiProperty;
import org.apache.sling.hapi.HApiType;
import org.apache.sling.hapi.HApiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@inheritDoc}
 */
public class HApiTypeLazyWrapper implements HApiType {
    public static final Logger LOG = LoggerFactory.getLogger(HApiTypeLazyWrapper.class);
    private final HApiUtil util;
    private final ResourceResolver resolver;
    private String serverUrl;
    private final Resource resource;

    /**
     * A new HApiType that is just a weak reference by name
     * @param util
     * @param resolver
     * @param name
     */
    public HApiTypeLazyWrapper(HApiUtil util, ResourceResolver resolver, String serverUrl, String name) {
        this.util = util;
        this.resolver = resolver;
        this.serverUrl = serverUrl;
        try {
            this.resource = util.getTypeResource(resolver, name);
        } catch (RepositoryException e) {
            throw new HApiException("Can't find type " + name + "!", e);
        }
    }

    public HApiTypeLazyWrapper(HApiUtil util, ResourceResolver resolver, String serverUrl, Resource resource) {
        this.util = util;
        this.resolver = resolver;
        this.serverUrl = serverUrl;
        this.resource = resource;
    }
    /**
     * Load the type from the cache
     * @return
     */
    private HApiType getTypeFromCache() {
        try {
            return TypesCache.getInstance(this.util).getType(resolver, resource);
        } catch (RepositoryException e) {
            String name = (null != resource) ? resource.getName() : "";
            throw new HApiException("Can't find type " + name + "!", e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getTypeFromCache().getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return getTypeFromCache().getDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return getTypeFromCache().getPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl() {
        return this.serverUrl + getPath() + ".html";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFqdn() {
        return getTypeFromCache().getFqdn();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getParameters() {
        return getTypeFromCache().getParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, HApiProperty> getProperties() {
        return getTypeFromCache().getProperties();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, HApiProperty> getAllProperties() {
        return getAllProperties(this);
    }

    private Map<String, HApiProperty> getAllProperties(HApiType rootType) {
        HApiType parent = getParent(rootType);
        Map<String, HApiProperty> allProps = new HashMap<String, HApiProperty>();
        LOG.debug("parent: {}", parent);
        if (null != parent) {
            Map<String, HApiProperty> parentProps;
            if (parent instanceof HApiTypeLazyWrapper) {
                parentProps = ((HApiTypeLazyWrapper) parent).getAllProperties(rootType);
            } else {
                parentProps = parent.getAllProperties();
            }
            LOG.debug("parent props: {}", parentProps);
            allProps.putAll(parentProps);
        }
        allProps.putAll(getProperties());
        return allProps;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public HApiType getParent() {
        return getTypeFromCache().getParent();
    }

    private HApiType getParent(HApiType rootType) {
        if (this.equals(rootType)) return null;
        return getParent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAbstract() {
        return getTypeFromCache().isAbstract();
    }

    @Override
    public String toString() {
        return "[Weak reference] "  + this.getName() + "(" + this.getPath() + "): Properties: " + this.getProperties();
    }
}
