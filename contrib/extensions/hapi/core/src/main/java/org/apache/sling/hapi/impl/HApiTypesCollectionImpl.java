/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.hapi.impl;

import org.apache.sling.hapi.HApiType;
import org.apache.sling.hapi.HApiTypesCollection;

import java.util.ArrayList;

public class HApiTypesCollectionImpl extends ArrayList<HApiType> implements HApiTypesCollection {

    private final String name;
    private final String description;
    private final String path;
    private final String serverUrl;
    private final String fqdn;

    /**
     *
     * @param name The name of the collection.
     * @param description The description of the collection
     * @param path The path of the resource describing the collection
     */
    public HApiTypesCollectionImpl(String name, String description, String serverUrl, String path, String fqdn) {
        this.name = name;
        this.description = description;
        this.serverUrl = serverUrl.substring(0, serverUrl.length() - (serverUrl.endsWith("/") ? 1 : 0));
        this.path = path;
        this.fqdn = fqdn;
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
}
