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
package org.apache.sling.distribution.packaging.impl.importer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.component.DistributionComponent;
import org.apache.sling.distribution.component.DistributionComponentProvider;
import org.apache.sling.distribution.component.impl.DefaultDistributionComponentFactoryConstants;
import org.apache.sling.distribution.component.impl.DistributionComponentFactoryManager;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageImportException;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote implementation of {@link org.apache.sling.distribution.packaging.DistributionPackageImporter}
 */
@Component(label = "Sling Distribution - Remote Package Importer Factory",
        metatype = true,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = DistributionPackageImporter.class)
public class RemoteDistributionPackageImporterFactory implements DistributionPackageImporter, DistributionComponentProvider {
    private static final String TRANSPORT_AUTHENTICATION_PROVIDER_TARGET = DefaultDistributionComponentFactoryConstants.COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER + ".target";


    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = DefaultDistributionComponentFactoryConstants.PACKAGE_IMPORTER_REMOTE, propertyPrivate = true)
    private static final String TYPE = DefaultDistributionComponentFactoryConstants.COMPONENT_TYPE;

    @Property
    private static final String NAME = DefaultDistributionComponentFactoryConstants.COMPONENT_NAME;

    @Property(name = TRANSPORT_AUTHENTICATION_PROVIDER_TARGET)
    @Reference(name = "TransportAuthenticationProvider", policy = ReferencePolicy.STATIC)
    private volatile TransportAuthenticationProvider transportAuthenticationProvider;

    @Property(cardinality = 100)
    public static final String ENDPOINTS = DefaultDistributionComponentFactoryConstants.PACKAGE_IMPORTER_REMOTE_PROPERTY_ENDPOINTS;

    @Property(options = {
            @PropertyOption(name = "All",
                    value = "all endpoints"
            ),
            @PropertyOption(name = "One",
                    value = "one endpoint"
            )},
            value = "One"
    )
    private static final String ENDPOINT_STRATEGY = DefaultDistributionComponentFactoryConstants.PACKAGE_IMPORTER_REMOTE_PROPERTY_ENDPOINTS_STRATEGY;

    private DistributionPackageImporter importer;

    @Reference
    private DistributionComponentFactoryManager componentManager;


    @Activate
    protected void activate(Map<String, Object> config) {

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.putAll(config);

        properties.put(DefaultDistributionComponentFactoryConstants.COMPONENT_PROVIDER, this);

        importer = componentManager.createComponent(DistributionPackageImporter.class, properties);

    }

    public boolean importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionPackageImportException {
        return importer.importPackage(resourceResolver, distributionPackage);
    }

    public DistributionPackage importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionPackageImportException {
        return importer.importStream(resourceResolver, stream);
    }

    public <ComponentType extends DistributionComponent> ComponentType getComponent(@Nonnull Class<ComponentType> type,
                                                                                   @Nullable String componentName) {
        if (type.isAssignableFrom(TransportAuthenticationProvider.class)) {
            return (ComponentType) transportAuthenticationProvider;
        }

        return null;
    }
}
