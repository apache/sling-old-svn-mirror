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

import java.util.Iterator;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.component.impl.DistributionComponentProvider;
import org.apache.sling.distribution.component.impl.DistributionComponentUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;


@Component(label = "Distribution Service Resource Provider Factory",
        description = "Distribution Service Resource Provider Factory",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true)
@Service(value = ResourceProvider.class)
@Properties({
        @Property(name = ResourceProvider.ROOTS),
        @Property(name = ResourceProvider.OWNS_ROOTS, boolValue = true, propertyPrivate = true)
})
public class DistributionServiceResourceProviderFactory implements ResourceProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());


    @Property
    public final static String KIND = DistributionComponentUtils.PN_KIND;

    /**
     * resourceProperties contains the list of properties returned by this provider.
     * Properties can be configured for the main resource, for the root resource and for child resources.
     * Root resource properties are static and can be configured as follows:
     * ../rootResourcePropertyName = rootResourcePropertyValue
     * <p/>
     * Main resource properties can be static or dynamic (depending on the underlying resource) are configured as follows:
     * mainResourceStaticPropertyName = mainResourceStaticPropertyValue
     * mainResourceDynamicPropertyName = {mainResourceSourcePropertyName}
     * <p/>
     * Child resource properties are static an can be configured as follows:
     * childResourceName/childResourcePropertyName=childResourcePropertyValue
     */
    @Property(cardinality = Integer.MAX_VALUE)
    public final static String RESOURCE_PROPERTIES = "resourceProperties";

    @Reference
    DistributionComponentProvider componentProvider;

    private ResourceProvider resourceProvider;

    @Activate
    public void activate(BundleContext context, Map<String, Object> properties) {

        log.debug("activating resource provider with config {}", properties);

        String kind = PropertiesUtil.toString(properties.get(KIND), null);
        String resourceRoot = PropertiesUtil.toString(properties.get(ResourceProvider.ROOTS), null);


        Map<String, String> additionalResourceProperties = PropertiesUtil.toMap(properties.get(RESOURCE_PROPERTIES), new String[0]);

        resourceProvider = new DistributionServiceResourceProvider(kind,
                componentProvider,
                resourceRoot,
                additionalResourceProperties);

        log.debug("created resource provider {}", resourceProvider);
    }

    @Deactivate
    public void deactivate(BundleContext context) {
        resourceProvider = null;
    }


    public Resource getResource(ResourceResolver resourceResolver, HttpServletRequest request, String path) {
        return getResource(resourceResolver, path);
    }

    public Resource getResource(ResourceResolver resourceResolver, String path) {
        return resourceProvider.getResource(resourceResolver, path);
    }

    public Iterator<Resource> listChildren(Resource parent) {
        return resourceProvider.listChildren(parent);
    }
}
