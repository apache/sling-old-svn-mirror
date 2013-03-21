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
package org.apache.sling.serviceusermapping.impl;

import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;

class ServiceUserMapperImpl implements ServiceUserMapper {

    private String bundleServiceName;

    private final ServiceUserMapperController controller;

    ServiceUserMapperImpl(final Bundle bundle, final ServiceUserMapperController controller) {
        final String name = (String) bundle.getHeaders().get("Sling-ResourceResolver-Service");
        if (name != null && name.trim().length() > 0) {
            this.bundleServiceName = name.trim();
        } else {
            this.bundleServiceName = bundle.getSymbolicName();
        }

        this.controller = controller;
    }

    public String getServiceName(String serviceInfo) {
        return controller.getServiceName(this.bundleServiceName, serviceInfo);
    }

    public String getUserForService(String serviceInfo) {
        return controller.getUserForService(this.bundleServiceName, serviceInfo);
    }

}
