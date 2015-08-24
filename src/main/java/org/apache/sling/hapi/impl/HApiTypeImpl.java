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

    private final HApiType parent;

    private String name;

    private String description;
    private String path;
    private String fqdn;
    private List<String> parameters;
    private Map<String, HApiProperty> properties;
    private boolean isAbstract;

    /**
     * A new HApiType
     * @param name
     * @param description
     * @param path
     * @param fqdn
     * @param parameters
     * @param properties
     * @param parent
     */
    public HApiTypeImpl(String name, String description, String path, String fqdn, List<String> parameters, Map<String,
            HApiProperty> properties, HApiType parent, boolean isAbstract) {
        this.name = name;
        this.description = description;
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
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    public String getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     */
    public String getUrl() {
        return getPath() + ".html";
    }

    /**
     * {@inheritDoc}
     */
    public String getFqdn() {
        return fqdn;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, HApiProperty> getProperties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     */
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
    public HApiType getParent() {
        return parent;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAbstract() {
        return isAbstract;
    }

}
