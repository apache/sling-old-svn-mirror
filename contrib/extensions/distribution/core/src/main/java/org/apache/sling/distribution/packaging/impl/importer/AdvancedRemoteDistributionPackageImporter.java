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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.core.DistributionTransport;
import org.apache.sling.distribution.transport.impl.AdvancedHttpDistributionTransport;
import org.apache.sling.distribution.transport.impl.DistributionEndpoint;
import org.apache.sling.distribution.transport.impl.MultipleEndpointDistributionTransport;
import org.apache.sling.distribution.transport.impl.TransportEndpointStrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link org.apache.sling.distribution.packaging.DistributionPackageImporter} supporting multiple
 * endpoints and custom HTTP headers and body.
 */
//FIXME: not yet used and should be cleaned up
public class AdvancedRemoteDistributionPackageImporter implements DistributionPackageImporter {


    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * name of this importer.
     */
    @Property(label = "Name", description = "The name of the importer.")
    public static final String NAME = DistributionComponentConstants.PN_NAME;

    @Property(name = "transportSecretProvider.target")
    @Reference(name = "transportSecretProvider")
    private volatile DistributionTransportSecretProvider distributionTransportSecretProvider;

    @Property(cardinality = 100)
    public static final String ENDPOINTS = "endpoints";

    @Property(options = {
            @PropertyOption(name = "All",
                    value = "all endpoints"
            ),
            @PropertyOption(name = "One",
                    value = "one endpoint"
            )},
            value = "One"
    )
    private static final String ENDPOINT_STRATEGY = "endpoint.strategy";

    @Property(boolValue = false)
    private static final String USE_CUSTOM_HEADERS = "useCustomHeaders";

    @Property(cardinality = 50)
    private static final String CUSTOM_HEADERS = "customHeaders";

    @Property(boolValue = false)
    private static final String USE_CUSTOM_BODY = "useCustomBody";

    @Property
    private static final String CUSTOM_BODY = "customBody";

    @Reference
    private DistributionEventFactory distributionEventFactory;

    private DistributionTransport transportHandler;

    @Activate
    protected void activate(Map<String, ?> config) throws Exception {

        String[] endpoints = PropertiesUtil.toStringArray(config.get(ENDPOINTS), new String[0]);
        endpoints = SettingsUtils.removeEmptyEntries(endpoints);

        String endpointStrategyName = PropertiesUtil.toString(config.get(ENDPOINT_STRATEGY),
                TransportEndpointStrategyType.One.name());
        TransportEndpointStrategyType transportEndpointStrategyType = TransportEndpointStrategyType.valueOf(endpointStrategyName);


        boolean useCustomHeaders = PropertiesUtil.toBoolean(config.get(USE_CUSTOM_HEADERS), false);
        String[] customHeaders = PropertiesUtil.toStringArray(config.get(CUSTOM_HEADERS), new String[0]);
        customHeaders = SettingsUtils.removeEmptyEntries(customHeaders);

        boolean useCustomBody = PropertiesUtil.toBoolean(config.get(USE_CUSTOM_BODY), false);
        String customBody = PropertiesUtil.toString(config.get(CUSTOM_BODY), "");


        String importerName = PropertiesUtil.toString(config.get(NAME), null);

        DefaultDistributionLog distributionLog = new DefaultDistributionLog(DistributionComponentKind.IMPORTER, importerName, RemoteDistributionPackageImporter.class, DefaultDistributionLog.LogLevel.ERROR);


        List<DistributionTransport> transportHandlers = new ArrayList<DistributionTransport>();

        for (String endpoint : endpoints) {
            if (endpoint != null && endpoint.length() > 0) {
                transportHandlers.add(new AdvancedHttpDistributionTransport(distributionLog, useCustomHeaders, customHeaders,
                        useCustomBody, customBody,
                        new DistributionEndpoint(endpoint), null, distributionTransportSecretProvider, -1));
            }
        }

        transportHandler = new MultipleEndpointDistributionTransport(transportHandlers,
                transportEndpointStrategyType);

    }


    public void importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) {
        try {
            transportHandler.deliverPackage(resourceResolver, distributionPackage);
        } catch (Exception e) {
            log.error("failed delivery", e);
        }
    }

    public DistributionPackageInfo importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionException {
        throw new DistributionException("not supported");
    }

}
