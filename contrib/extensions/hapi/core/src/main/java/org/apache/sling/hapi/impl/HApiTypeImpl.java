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

import org.apache.sling.hapi.HApiProperty;
import org.apache.sling.hapi.HApiType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * {@inheritDoc}
 */
public class HApiTypeImpl implements HApiType {

    public static final Logger LOG = LoggerFactory.getLogger(HApiTypeImpl.class);
    private HApiType parent;

    private String name;

    private String description;

    private final String serverUrl;
    private String path;
    private String fqdn;
    private List<String> parameters;
    private Map<String, HApiProperty> properties;
    private boolean isAbstract;


    /**
     * A new HApiType
     * @param name
     * @param description
     * @param serverUrl
     * @param path
     * @param fqdn
     * @param parameters
     * @param properties
     * @param parent
     */
    public HApiTypeImpl(String name, String description, String serverUrl, String path, String fqdn, List<String> parameters, Map<String,
            HApiProperty> properties, HApiType parent, boolean isAbstract) {
        this.name = name;
        this.description = description;
        this.serverUrl = serverUrl.substring(0, serverUrl.length() - (serverUrl.endsWith("/") ? 1 : 0));
        this.path = path;
        this.fqdn = fqdn;
        this.parameters = parameters;
        this.properties = properties;
        this.parent = parent;
        this.isAbstract = isAbstract;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return path;
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
        return fqdn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, HApiProperty> getProperties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, HApiProperty> getAllProperties() {
        Map<String, HApiProperty> allProps = new HashMap<String, HApiProperty>();
        LOG.debug("parent: {}", parent);
        if (null != parent) {
            Map<String, HApiProperty> parentProps = parent.getAllProperties();
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
        return parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAbstract() {
        return isAbstract;
    }


    public void setParent(HApiType parent) {
        this.parent = parent;
    }

    public void setProperties(Map<String, HApiProperty> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return this.getName() + ": Properties: " + this.getProperties();
    }
}
