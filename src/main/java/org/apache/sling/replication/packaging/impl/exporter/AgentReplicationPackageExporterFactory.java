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
package org.apache.sling.replication.packaging.impl.exporter;

import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.component.ReplicationComponentFactory;
import org.apache.sling.replication.component.ReplicationComponentProvider;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(label = "Sling Replication - Agent Based Package Exporter",
        metatype = true,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = ReplicationPackageExporter.class)
public class AgentReplicationPackageExporterFactory implements ReplicationPackageExporter, ReplicationComponentProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = ReplicationComponentFactory.PACKAGE_EXPORTER_AGENT, propertyPrivate = true)
    private static final String TYPE = ReplicationComponentFactory.COMPONENT_TYPE;

    @Property
    private static final String NAME = ReplicationComponentFactory.COMPONENT_NAME;

    @Property(label = "Target ReplicationAgent", name = "ReplicationAgent.target")
    @Reference(name = "ReplicationAgent", policy = ReferencePolicy.STATIC)
    private ReplicationAgent agent;

    @Reference
    ReplicationComponentFactory replicationComponentFactory;

    ReplicationPackageExporter packageExporter;


    @Activate
    public void activate(Map<String, Object> config) throws Exception {

        packageExporter = replicationComponentFactory.createComponent(ReplicationPackageExporter.class, config, this);
    }

    public List<ReplicationPackage> exportPackage(ResourceResolver resourceResolver, ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {

       return packageExporter.exportPackage(resourceResolver, replicationRequest);
    }

    public ReplicationPackage exportPackageById(ResourceResolver resourceResolver, String replicationPackageId) {
        return packageExporter.exportPackageById(resourceResolver, replicationPackageId);
    }

    public <ComponentType> ComponentType getComponent(Class<ComponentType> type, String componentName) {
        if (type.isAssignableFrom(ReplicationAgent.class)) {
            return (ComponentType) agent;
        }
        return null;
    }
}
