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

package org.apache.sling.distribution.resources.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.DistributionConfigurationManager;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ResourceProviderFactory} for distribution configuration resources.
 */
@Component(metatype = true,
        label = "Apache Sling Distribution Resources - Configuration Resource Provider Factory",
        description = "Distribution Configuration Resource Provider Factory",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = ResourceProviderFactory.class)
@Properties({
        @Property(name = ResourceProvider.ROOTS),
        @Property(name = ResourceProvider.OWNS_ROOTS, boolValue = true, propertyPrivate = true)
})
@Property(name="webconsole.configurationFactory.nameHint", value="Resource kind: {kind}")
public class DistributionConfigurationResourceProviderFactory implements ResourceProviderFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property
    private final static String KIND = DistributionComponentConstants.PN_KIND;

    @Reference
    private
    DistributionConfigurationManager configurationManager;

    private String resourceRoot;
    private String kind;

    @Activate
    public void activate(BundleContext context, Map<String, Object> properties) {

        log.debug("activating resource provider with config {}", properties);

        resourceRoot = PropertiesUtil.toString(properties.get(ResourceProvider.ROOTS), null);
        kind = PropertiesUtil.toString(properties.get(KIND), null);

    }

    @Deactivate
    public void deactivate(BundleContext context) {

    }

    public ResourceProvider getResourceProvider(Map<String, Object> authenticationInfo) throws LoginException {
        return new DistributionConfigurationResourceProvider(configurationManager, kind, resourceRoot);
    }

    public ResourceProvider getAdministrativeResourceProvider(Map<String, Object> authenticationInfo) throws LoginException {
        return getResourceProvider(authenticationInfo);
    }
}
