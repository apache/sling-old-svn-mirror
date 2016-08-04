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
package org.apache.sling.distribution.packaging.impl.exporter;

import javax.annotation.Nonnull;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageProcessor;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi configuration factory for {@link RemoteDistributionPackageExporter}s.
 */
@Component(label = "Apache Sling Distribution Exporter - Remote Package Exporter Factory",
        metatype = true,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Service(value = DistributionPackageExporter.class)
@Property(name="webconsole.configurationFactory.nameHint", value="Exporter name: {name}")
public class RemoteDistributionPackageExporterFactory implements DistributionPackageExporter {


    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * name of this exporter.
     */
    @Property(label = "Name", description = "The name of the exporter.")
    private static final String NAME = DistributionComponentConstants.PN_NAME;

    /**
     * endpoints property
     */
    @Property(cardinality = 100, label = "Endpoints", description = "The list of endpoints from which the packages will be exported.")
    private static final String ENDPOINTS = "endpoints";

    /**
     * no. of items to poll property
     */
    @Property(label = "Pull Items", description = "number of subsequent pull requests to make", intValue = 1)
    private static final String PULL_ITEMS = "pull.items";

    @Property(name = "packageBuilder.target", label = "Package Builder", description = "The target reference for the DistributionPackageBuilder used to create distribution packages, " +
            "e.g. use target=(name=...) to bind to services by name.", value = SettingsUtils.COMPONENT_NAME_DEFAULT)
    @Reference(name = "packageBuilder")
    private DistributionPackageBuilder packageBuilder;


    @Property(name = "transportSecretProvider.target", label = "Transport Secret Provider", description = "The target reference for the DistributionTransportSecretProvider used to obtain the credentials used for accessing the remote endpoints, " +
            "e.g. use target=(name=...) to bind to services by name.", value = SettingsUtils.COMPONENT_NAME_DEFAULT)
    @Reference(name = "transportSecretProvider")
    private
    DistributionTransportSecretProvider transportSecretProvider;

    private DistributionPackageExporter exporter;

    @Activate
    protected void activate(Map<String, Object> config) throws Exception {
        log.info("activating remote exporter with pb {} and dtsp {}", packageBuilder, transportSecretProvider);

        String[] endpoints = PropertiesUtil.toStringArray(config.get(ENDPOINTS), new String[0]);
        endpoints = SettingsUtils.removeEmptyEntries(endpoints);

        int pollItems = PropertiesUtil.toInteger(config.get(PULL_ITEMS), Integer.MAX_VALUE);



        String exporterName = PropertiesUtil.toString(config.get(NAME), null);

        DefaultDistributionLog distributionLog = new DefaultDistributionLog(DistributionComponentKind.EXPORTER, exporterName, RemoteDistributionPackageExporter.class, DefaultDistributionLog.LogLevel.ERROR);


        exporter = new RemoteDistributionPackageExporter(distributionLog, packageBuilder, transportSecretProvider, endpoints, pollItems);
    }


    @Deactivate
    protected void deactivate() {
        exporter = null;
    }

    public void exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest, @Nonnull DistributionPackageProcessor packageProcessor) throws DistributionException {
        exporter.exportPackages(resourceResolver, distributionRequest, packageProcessor);
    }

    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String distributionPackageId) throws DistributionException {
        return exporter.getPackage(resourceResolver, distributionPackageId);
    }


}
