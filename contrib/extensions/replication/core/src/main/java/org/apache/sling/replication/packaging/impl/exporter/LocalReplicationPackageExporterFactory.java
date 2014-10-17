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
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.agent.ReplicationComponentFactory;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.replication.packaging.ReplicationPackageExporter} implementation which creates a FileVault based
 * {@link org.apache.sling.replication.packaging.ReplicationPackage} locally.
 */
@Component(label = "Sling Replication - Local Package Exporter Factory",
        metatype = true,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = ReplicationPackageExporter.class)
public class LocalReplicationPackageExporterFactory implements ReplicationPackageExporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = ReplicationComponentFactory.PACKAGE_EXPORTER_LOCAL, propertyPrivate = true)
    private static final String TYPE = ReplicationComponentFactory.COMPONENT_TYPE;

    @Property
    private static final String NAME = ReplicationComponentFactory.COMPONENT_NAME;

    @Reference
    ReplicationComponentFactory replicationComponentFactory;

    ReplicationPackageExporter exporter;

    @Activate
    public void activate(Map<String, Object> config) {
        exporter = replicationComponentFactory.createComponent(ReplicationPackageExporter.class, config, null);
    }


    public List<ReplicationPackage> exportPackage(ResourceResolver resourceResolver, ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {
        return exporter.exportPackage(resourceResolver, replicationRequest);
    }

    public ReplicationPackage exportPackageById(ResourceResolver resourceResolver, String replicationPackageId) {
        return exporter.exportPackageById(resourceResolver, replicationPackageId);
    }
}
