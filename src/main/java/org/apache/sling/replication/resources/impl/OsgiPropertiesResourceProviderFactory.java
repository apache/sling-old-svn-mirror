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

package org.apache.sling.replication.resources.impl;

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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Component(label="Osgi Service Properties Resource Provider Factory",
        description="Osgi Service Properties Resource Provider Factory",
        configurationFactory=true,
        policy= ConfigurationPolicy.REQUIRE,
        name = OsgiPropertiesResourceProviderFactory.SERVICE_PID,
        metatype=true)
@Service(value=ResourceProviderFactory.class)
@Properties({
        @Property(name= ResourceProvider.ROOTS),
        @Property(name = ResourceProvider.OWNS_ROOTS, boolValue=true, propertyPrivate=true)
})
public class OsgiPropertiesResourceProviderFactory implements ResourceProviderFactory {
    public final static String SERVICE_PID = "org.apache.sling.replication.resources.impl.OsgiPropertiesResourceProviderFactory";

    /**
     * The name of the class that is used for resource resolution.
     * For Osgi configuration this is the factoryPid.
     * For Osgi services this is the service interface.
     */
    @Property
    public final static String SERVICE_INTERFACE = "serviceType";

    public final static String DEFAULT_FRIENDLY_NAME_PROPERTY = "name";
    /**
     * nameProperty contains the name of the property that will be used to expose
     * the underlying resources.
     *
     * resourceRoot/resourceName/childResourceName
     */
    @Property(value = DEFAULT_FRIENDLY_NAME_PROPERTY)
    public final static String FRIENDLY_NAME_PROPERTY = "nameProperty";

    /**
     * resourceProperties contains the list of properties returned by this provider.
     * Properties can be configured for the main resource, for the root resource and for child resources.
     * Root resource properties are static and can be configured as follows:
     *      ../rootResourcePropertyName = rootResourcePropertyValue
     *
     * Main resource properties can be static or dynamic (depending on the underlying resource) are configured as follows:
     *      mainResourceStaticPropertyName = mainResourceStaticPropertyValue
     *      mainResourceDynamicPropertyName = {mainResourceSourcePropertyName}
     *
     * Child resource properties are static an can be configured as follows:
     *      childResourceName/childResourcePropertyName=childResourcePropertyValue
     *
     */
    @Property(cardinality = 100)
    public final static String RESOURCE_PROPERTIES = "resourceProperties";

    public final static String DEFAULT_PROVIDER_TYPE = "osgiService";
    /**
     * The providerType can be osgiService or osgiConfig.
     * A provider of type osgiService will allow read only access to osgi service properties of a particuar interface.
     * The resource can be adapted to the underlying service instance.
     *
     * A provider of type osgiConfig will allow CRUD access to osgi configurations registered for a particular factory.     *
     */
    @Property(value = DEFAULT_PROVIDER_TYPE)
    public final static String PROVIDER_TYPE = "providerType";

    @Reference
    public ConfigurationAdmin configurationAdmin;

    private ServiceTracker serviceTracker;
    private ResourceProvider resourceProvider;

    @Activate
    public void activate(BundleContext context, Map<String, Object> properties) {
        String friendlyNameProperty = PropertiesUtil.toString(properties.get(FRIENDLY_NAME_PROPERTY), DEFAULT_FRIENDLY_NAME_PROPERTY);
        String type = PropertiesUtil.toString(properties.get(SERVICE_INTERFACE), null);
        String resourceRoot = PropertiesUtil.toString(properties.get(ResourceProvider.ROOTS), null);

        Map<String, String> additionalResourceProperties = PropertiesUtil.toMap(properties.get(RESOURCE_PROPERTIES),
                new String[]{friendlyNameProperty + "=" + friendlyNameProperty});
        boolean isConfig = !DEFAULT_PROVIDER_TYPE.equalsIgnoreCase(PropertiesUtil.toString(properties.get(PROVIDER_TYPE), DEFAULT_PROVIDER_TYPE));


        if (isConfig) {
            resourceProvider = new OsgiConfigurationResourceProvider(configurationAdmin,
                    type,
                    friendlyNameProperty,
                    resourceRoot,
                    additionalResourceProperties);

        }
        else {
            OsgiServicePropertiesResourceProvider servicePropertiesResourceProvider;
            resourceProvider = servicePropertiesResourceProvider = new OsgiServicePropertiesResourceProvider(context,
                    type,
                    friendlyNameProperty,
                    resourceRoot,
                    additionalResourceProperties);

            serviceTracker = new ServiceTracker(context, type, servicePropertiesResourceProvider);
            serviceTracker.open();
        }
    }

    @Deactivate
    public void deactivate(BundleContext context) {
        if (serviceTracker != null) {
            serviceTracker.close();
            serviceTracker = null;
        }
    }

    public ResourceProvider getResourceProvider(Map<String, Object> authenticationInfo) throws LoginException {
        return resourceProvider;
    }

    public ResourceProvider getAdministrativeResourceProvider(Map<String, Object> authenticationInfo) throws LoginException {
        return getResourceProvider(authenticationInfo);
    }
}
