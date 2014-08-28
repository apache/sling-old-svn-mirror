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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(label = "Agent Based Replication Package Exporter", configurationFactory = true)
@Service(value = ReplicationPackageExporter.class)
public class AgentReplicationPackageExporter implements ReplicationPackageExporter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property
    private static final String NAME = "name";

    @Property(value = "exporters/agent", propertyPrivate = true)
    private static final String FACTORY_NAME = "factoryName";

    @Property(label = "Queue")
    private static final String QUEUE_NAME = "queue";

    @Property(label = "Target ReplicationAgent", name = "ReplicationAgent.target")
    @Reference(name = "ReplicationAgent", policy = ReferencePolicy.STATIC)
    private ReplicationAgent agent;

    @Property(label = "Target ReplicationPackageBuilder", name = "ReplicationPackageBuilder.target")
    @Reference(name = "ReplicationPackageBuilder", policy = ReferencePolicy.STATIC)
    private ReplicationPackageBuilder replicationPackageBuilder;

    private String queueName;

    @Activate
    public void activate(Map<String, ?> config) throws Exception {
        queueName = PropertiesUtil.toString(config.get(QUEUE_NAME), "");
    }

    public List<ReplicationPackage> exportPackage(ReplicationRequest replicationRequest) {

        List<ReplicationPackage> result = new ArrayList<ReplicationPackage>();
        try {
            log.info("getting item from queue {}", queueName);

            ReplicationQueue queue = agent.getQueue(queueName);
            ReplicationQueueItem info = queue.getHead();
            ReplicationPackage replicationPackage = null;
            if (info != null) {
                queue.removeHead();
                replicationPackage = replicationPackageBuilder.getPackage(info.getId());
                if (replicationPackage != null) {
                    result.add(replicationPackage);
                }
            }

        } catch (Exception ex) {
            log.error("Error exporting package", ex);
        }

        return result;
    }

    public ReplicationPackage exportPackageById(String replicationPackageId) {
        return null;
    }
}
