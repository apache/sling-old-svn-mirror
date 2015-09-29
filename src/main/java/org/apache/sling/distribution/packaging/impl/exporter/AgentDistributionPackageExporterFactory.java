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
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExportException;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.serialization.DistributionPackageBuilderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(label = "Apache Sling Distribution Exporter - Agent Based Package Exporter",
        metatype = true,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = DistributionPackageExporter.class)
public class AgentDistributionPackageExporterFactory implements DistributionPackageExporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * name of this exporter.
     */
    @Property(label = "Name", description = "The name of the exporter.")
    public static final String NAME = DistributionComponentConstants.PN_NAME;

    @Property(label = "Queue", description = "The name of the queue from which the packages should be exported.")
    private static final String QUEUE_NAME = "queue";

    @Property(name = "agent.target", label = "The target reference for the DistributionAgent that will be used to export packages.")
    @Reference(name = "agent")
    private DistributionAgent agent;


    @Reference
    private DistributionPackageBuilderProvider packageBuilderProvider;

    private DistributionPackageExporter packageExporter;


    @Activate
    public void activate(Map<String, Object> config) throws Exception {

        String queueName = PropertiesUtil.toString(config.get(QUEUE_NAME), "");
        String name = PropertiesUtil.toString(config.get(NAME), "");


        packageExporter = new AgentDistributionPackageExporter(queueName, agent, packageBuilderProvider, name);
    }

    @Nonnull
    public List<DistributionPackage> exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) throws DistributionPackageExportException {
        return packageExporter.exportPackages(resourceResolver, distributionRequest);
    }

    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String distributionPackageId) {
        return packageExporter.getPackage(resourceResolver, distributionPackageId);
    }

}
