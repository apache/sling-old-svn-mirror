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
package org.apache.sling.distribution.agent.impl;

import javax.annotation.Nonnull;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.component.impl.DistributionComponentUtils;
import org.osgi.framework.BundleContext;

@Component(metatype = true,
        label = "Sling Distribution - Privilege Request Authorization Strategy",
        description = "OSGi configuration for request based authorization strategy based on privileges",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
@Service(DistributionRequestAuthorizationStrategy.class)
public class PrivilegeDistributionRequestAuthorizationStrategyFactory implements DistributionRequestAuthorizationStrategy {

    /**
     * name of this component.
     */
    @Property
    public static final String NAME = DistributionComponentUtils.NAME;

    /**
     * privilege request authorization strategy jcr privilege property
     */
    @Property
    public static final String REQUEST_AUTHORIZATION_STRATEGY_PRIVILEGE_PROPERTY_JCR_PRIVILEGE = "jcrPrivilege";


    DistributionRequestAuthorizationStrategy authorizationStrategy;

    @Activate
    public void activate(BundleContext context, Map<String, Object> config) {
        String jcrPrivilege = PropertiesUtil.toString(config.get(REQUEST_AUTHORIZATION_STRATEGY_PRIVILEGE_PROPERTY_JCR_PRIVILEGE), null);
        authorizationStrategy = new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege);
    }

    public void checkPermission(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) throws DistributionRequestAuthorizationException {
        authorizationStrategy.checkPermission(resourceResolver, distributionRequest);
    }
}
