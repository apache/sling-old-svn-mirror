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
package org.apache.sling.replication.serialization.impl.importer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.event.ReplicationEventType;
import org.apache.sling.replication.serialization.*;
import org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link org.apache.sling.replication.serialization.ReplicationPackageImporter}
 */
@Component(label = "Default Replication Package Importer")
@Service(value = ReplicationPackageImporter.class)
@Property(name = "name", value = DefaultReplicationPackageImporter.NAME)
public class DefaultReplicationPackageImporter implements ReplicationPackageImporter {

    public static final String NAME = "default";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(name = "ReplicationPackageBuilder",
            target = "(name=" + FileVaultReplicationPackageBuilder.NAME + ")",
            policy = ReferencePolicy.DYNAMIC)
    private ReplicationPackageBuilder replicationPackageBuilder;

    @Reference
    private ReplicationEventFactory replicationEventFactory;


    public boolean importPackage(ReplicationPackage replicationPackage) {
        boolean success = false;
        try {
            success = replicationPackageBuilder.installPackage(replicationPackage);

            if (success) {
                log.info("replication package read and installed for path(s) {}", Arrays.toString(replicationPackage.getPaths()));

                Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
                dictionary.put("replication.action", replicationPackage.getAction());
                dictionary.put("replication.path", replicationPackage.getPaths());
                replicationEventFactory.generateEvent(ReplicationEventType.PACKAGE_INSTALLED, dictionary);

                replicationPackage.delete();
            } else {
                log.warn("could not read a replication package");
            }
        } catch (Exception e) {
            log.error("cannot import a package from the given stream of type {}", replicationPackage.getType());
        }
        return success;
    }

    public ReplicationPackage readPackage(InputStream stream) throws ReplicationPackageReadingException {
        try {
            ReplicationPackage replicationPackage = replicationPackageBuilder.readPackage(stream);

            return replicationPackage;


        } catch (Exception e) {
            log.error("cannot read a package from the given stream");
        }
        return null;
    }

}
