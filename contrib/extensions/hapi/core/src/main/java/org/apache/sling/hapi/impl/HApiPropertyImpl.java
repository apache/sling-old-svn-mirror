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

import org.apache.sling.hapi.HApiType;
import org.apache.sling.hapi.HApiProperty;

/**
 * {@inheritDoc}
 */
public class HApiPropertyImpl implements HApiProperty {
    private String name;
    private String description;
    private HApiType type;
    private Boolean multiple;

    /**
     *
     * @param name
     * @param description
     * @param type
     * @param multiple
     */
    public HApiPropertyImpl(String name, String description, HApiType type, Boolean multiple) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.multiple = multiple;
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
    public void setName(String name) {
        this.name = name;
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
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     */
    public HApiType getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public void setType(HApiType type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean getMultiple() {
        return multiple;
    }

    /**
     * {@inheritDoc}
     */
    public void setMultiple(Boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    public String toString() {
        return "HApiProperty{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type.getPath() +
                ", multiple=" + multiple +
                '}';
    }
}
