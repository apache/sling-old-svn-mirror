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

import java.util.ArrayList;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = true, ds = true, policy = ConfigurationPolicy.REQUIRE)
public class ServiceUserMapperController {

    @Property(
            label = "Service Mappings",
            description = "Provides mappings from service name to user names. "
                + "Each entry is of the form 'serviceName [ \":\" serviceInfo ] \"=\" userName' "
                + "where serviceName and serviceInfo identify the service and userName would "
                + "defines the name of the user to provide to the service. Invalid entries are logged and ignored.",
            unbounded = PropertyUnbounded.ARRAY)
    private static final String PROP_SERVICE2USER_MAPPING = "user.mapping";

    private static final String[] PROP_SERVICE2USER_MAPPING_DEFAULT = {};

    private static final String PROP_DEFAULT_USER = "user.default";

    @Property(
            name = PROP_DEFAULT_USER,
            label = "Default User",
            description = "The name of the user to use as the default if no service mapping"
                + "applies. If this property is missing or empty the default user name reflects "
                + "an anonymous user.")
    private static final String PROP_DEFAULT_USER_DEFAULT = "";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Mapping[] serviceUserMappings;

    private String defaultUser;

    private ServiceRegistration serviceUserMapper;

    @Activate
    private void activate(ComponentContext ctx, final Map<String, Object> config) {
        this.configure(config);
        this.serviceUserMapper = ctx.getBundleContext().registerService(ServiceUserMapper.class.getName(),
            new ServiceFactory() {

                public Object getService(Bundle bundle, ServiceRegistration registration) {
                    return new ServiceUserMapperImpl(bundle, ServiceUserMapperController.this);
                }

                public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
                    // no cleanup required
                }
            }, ctx.getProperties());
    }

    @Modified
    private void configure(Map<String, Object> config) {
        final String[] props = PropertiesUtil.toStringArray(config.get(PROP_SERVICE2USER_MAPPING),
            PROP_SERVICE2USER_MAPPING_DEFAULT);

        ArrayList<Mapping> mappings = new ArrayList<Mapping>(props.length);
        for (String prop : props) {
            if (prop != null) {
                try {
                    Mapping mapping = new Mapping(prop);
                    mappings.add(mapping);
                } catch (IllegalArgumentException iae) {
                    log.info("configure: Ignoring '{}': {}", prop, iae.getMessage());
                }
            }
        }

        this.serviceUserMappings = mappings.toArray(new Mapping[mappings.size()]);
        this.defaultUser = PropertiesUtil.toString(config.get(PROP_DEFAULT_USER), PROP_DEFAULT_USER_DEFAULT);
    }

    @Deactivate
    private void deactivate() {
        if (this.serviceUserMapper != null) {
            this.serviceUserMapper.unregister();
            this.serviceUserMapper = null;
        }
    }

    String getServiceName(final String bundleServiceName, final String serviceInfo) {
        return (serviceInfo == null) ? bundleServiceName : bundleServiceName + ":" + serviceInfo;
    }

    String getUserForService(final String bundleServiceName, final String serviceInfo) {
        // try with serviceInfo first
        for (Mapping mapping : this.serviceUserMappings) {
            final String user = mapping.map(bundleServiceName, serviceInfo);
            if (user != null) {
                return user;
            }
        }

        // second round without serviceInfo
        for (Mapping mapping : this.serviceUserMappings) {
            final String user = mapping.map(bundleServiceName, null);
            if (user != null) {
                return user;
            }
        }

        // finally, fall back to default user
        return this.defaultUser;
    }
}
