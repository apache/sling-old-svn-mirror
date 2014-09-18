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
package org.apache.sling.replication.packaging.impl.importer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.event.ReplicationEventType;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.replication.packaging.ReplicationPackageImporter} implementation which imports a FileVault
 * based {@link ReplicationPackage} locally.
 */
@Component(label = "Default Replication Package Importer",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = ReplicationPackageImporter.class)
public class LocalReplicationPackageImporterFactory implements ReplicationPackageImporter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property
    private static final String NAME = "name";

    @Property(value = "importers/local", propertyPrivate = true)
    private static final String FACTORY_NAME = "factoryName";

    @Property(label = "Target ReplicationPackageBuilder", name = "ReplicationPackageBuilder.target")
    @Reference(name = "ReplicationPackageBuilder", policy = ReferencePolicy.STATIC)
    private ReplicationPackageBuilder packageBuilder;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    LocalReplicationPackageImporter importer;

    @Activate
    public void activate(Map<String, Object> config) {
        importer = getInstance(config, packageBuilder, replicationEventFactory);
    }

    public static LocalReplicationPackageImporter getInstance(Map<String, Object> config,
            ReplicationPackageBuilder packageBuilder, ReplicationEventFactory replicationEventFactory) {
        if (packageBuilder == null) {
            throw new IllegalArgumentException("A package builder is required");
        }
        return new LocalReplicationPackageImporter(packageBuilder, replicationEventFactory);

    }

    public boolean importPackage(ReplicationPackage replicationPackage) {
       return importer.importPackage(replicationPackage);
    }

    public ReplicationPackage readPackage(InputStream stream) throws ReplicationPackageReadingException {
        return importer.readPackage(stream);
    }

}
