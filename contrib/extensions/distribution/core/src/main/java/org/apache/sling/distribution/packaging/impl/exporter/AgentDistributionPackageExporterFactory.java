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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackageProcessor;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageBuilderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi configuration factory for {@link AgentDistributionPackageExporter}s
 */
@Component(label = "Apache Sling Distribution Exporter - Agent Based Package Exporter",
        metatype = true,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = DistributionPackageExporter.class)
@Property(name="webconsole.configurationFactory.nameHint", value="Exporter name: {name}")
public class AgentDistributionPackageExporterFactory implements DistributionPackageExporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * name of this exporter.
     */
    @Property(label = "Name", description = "The name of the exporter.")
    private static final String NAME = DistributionComponentConstants.PN_NAME;

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

        String queueName = PropertiesUtil.toString(config.get(QUEUE_NAME), DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME);
        String name = PropertiesUtil.toString(config.get(NAME), "");


        packageExporter = new AgentDistributionPackageExporter(queueName, agent, packageBuilderProvider, name);
    }

    public void exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest, @Nonnull DistributionPackageProcessor packageProcessor) throws DistributionException {
        packageExporter.exportPackages(resourceResolver, distributionRequest, packageProcessor);
    }

    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String distributionPackageId) throws DistributionException {
        return packageExporter.getPackage(resourceResolver, distributionPackageId);
    }

}
