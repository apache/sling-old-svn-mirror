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
package org.apache.sling.replication.serialization.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.event.ReplicationEventType;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuilderProvider;
import org.apache.sling.replication.serialization.ReplicationPackageImporter;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
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

    private static final String QUEUE_NAME = "replication-package-import";
    private static final String QUEUE_TOPIC = "org/apache/sling/replication/import";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReplicationPackageBuilderProvider replicationPackageBuilderProvider;

    @Reference
    private ReplicationEventFactory replicationEventFactory;


    public boolean importStream(InputStream stream, String type) {
        boolean success = false;
        try {
            ReplicationPackage replicationPackage = getReplicationPackage(stream, type, true);

            if (replicationPackage != null) {
                if (log.isInfoEnabled()) {
                    log.info("replication package read and installed for path(s) {}",
                            Arrays.toString(replicationPackage.getPaths()));
                }

                Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
                dictionary.put("replication.action", replicationPackage.getAction());
                dictionary.put("replication.path", replicationPackage.getPaths());
                replicationEventFactory.generateEvent(ReplicationEventType.PACKAGE_INSTALLED, dictionary);
                success = true;

                replicationPackage.delete();
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("could not read a replication package");
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("cannot import a package from the given stream of type {}", type);
            }
        }
        return success;
    }

    private ReplicationPackage getReplicationPackage(InputStream stream, String type, boolean install) throws ReplicationPackageReadingException {
        ReplicationPackage replicationPackage = null;
        if (type != null) {
            ReplicationPackageBuilder replicationPackageBuilder = replicationPackageBuilderProvider.getReplicationPackageBuilder(type);
            if (replicationPackageBuilder != null) {
                replicationPackage = replicationPackageBuilder.readPackage(stream, install);
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("cannot read streams of type {}", type);
                }
            }
        } else {
            throw new ReplicationPackageReadingException("could not get a replication package of type 'null'");
        }
        return replicationPackage;
    }



}
