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
import java.io.InputStream;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.component.impl.DistributionComponentManager;
import org.apache.sling.distribution.component.impl.DistributionComponentUtils;
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
public class RemoteDistributionPackageImporterFactory implements DistributionPackageImporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * name of this component.
     */
    @Property
    public static final String NAME = DistributionComponentUtils.NAME;

    /**
     * endpoints property
     */
    @Property(cardinality = -1)
    public static final String ENDPOINTS = "endpoints";

    /**
     * endpoint strategy property
     */
    @Property(options = {
            @PropertyOption(name = "All",
                    value = "all endpoints"
            ),
            @PropertyOption(name = "One",
                    value = "one endpoint"
            )},
            value = "One"
    )
    public static final String ENDPOINTS_STRATEGY = "endpoints.strategy";


    @Property(name = "transportAuthenticationProvider.target")
    @Reference(name = "transportAuthenticationProvider")
    TransportAuthenticationProvider transportAuthenticationProvider;
    private DistributionPackageImporter importer;

    @Reference
    private DistributionComponentManager componentManager;


    @Activate
    protected void activate(Map<String, Object> config) {

        String[] endpoints = PropertiesUtil.toStringArray(config.get(ENDPOINTS), new String[0]);
        String endpointStrategyName = PropertiesUtil.toString(config.get(ENDPOINTS_STRATEGY), "One");


        importer =  new RemoteDistributionPackageImporter(transportAuthenticationProvider, endpoints, endpointStrategyName);

    }

    public void importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionPackageImportException {
        importer.importPackage(resourceResolver, distributionPackage);
    }

    public DistributionPackage importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionPackageImportException {
        return importer.importStream(resourceResolver, stream);
    }
}
