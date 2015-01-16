/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.component.impl;


import java.util.Map;

/**
 * A distribution component holds a registered service that takes part in distribution process.
 * @param <ServiceType> the actual type of the service
 */
public class DistributionComponent<ServiceType> {

    private final DistributionComponentKind kind;
    private final String name;
    private final ServiceType service;
    private final Map<String, Object> properties;

    public DistributionComponent(DistributionComponentKind kind, String name, ServiceType service, Map<String, Object> properties) {
        this.kind = kind;
        this.name = name;

        this.service = service;
        this.properties = properties;
    }

    public ServiceType getService() {
        return service;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getName() {
        return name;
    }

    public DistributionComponentKind getKind() {
        return kind;
    }
}
