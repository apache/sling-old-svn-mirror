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
package org.apache.sling.replication.serialization.impl.exporter;


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageExporter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component(label = "Agent Based Replication Package Exporter")
@Service(value = ReplicationPackageExporter.class)
@Property(name = "name", value = AgentReplicationPackageExporter.NAME)
public class AgentReplicationPackageExporter implements ReplicationPackageExporter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String NAME = "agent";

    @Property(label = "Queue")
    private static final String QUEUE_NAME = "queue";

    @Property(label = "Target ReplicationAgent", name = "ReplicationAgent.target", value = "(name=reverse)")
    @Reference(name = "ReplicationAgent", target = "(name=reverse)", policy = ReferencePolicy.STATIC)
    private ReplicationAgent agent;

    @Property(label = "Target ReplicationPackageBuilder", name = "ReplicationPackageBuilder.target", value = "(name=vlt)")
    @Reference(name = "ReplicationPackageBuilder", target = "(name=vlt)", policy = ReferencePolicy.STATIC)
    private ReplicationPackageBuilder replicationPackageBuilder;


    private String queueName;



    @Activate
    public void activate(BundleContext context, Map<String, ?> config) throws Exception {
        queueName = PropertiesUtil.toString(config.get(QUEUE_NAME), "");
    }


    public ReplicationPackage exportPackage(ReplicationRequest replicationRequest) {

        try {
            log.info("getting item from queue {}", queueName);

            // get first item
            ReplicationPackage head = agent.removeHead(queueName);
            return head;
        }
        catch (Exception ex) {
            log.error("Error exporting package", ex);
        }

        return null;
    }

    public ReplicationPackage exportPackageById(String replicationPackageId) {
        return null;
    }
}
