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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuilderProvider;

/**
 * Default implementation of {@link org.apache.sling.replication.agent.ReplicationAgentsManager}
 */
@Component
@References({ 
    @Reference(name = "replicationPackageBuilder", 
               referenceInterface = ReplicationPackageBuilder.class, 
               cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, 
               policy = ReferencePolicy.DYNAMIC,
               bind = "bindReplicationPackageBuilder", 
               unbind = "unbindReplicationPackageBuilder")
    })
@Service(value = ReplicationPackageBuilderProvider.class)
public class DefaultReplicationPackageBuilderProvider implements ReplicationPackageBuilderProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, ReplicationPackageBuilder> replicationPackageBuilders = new HashMap<String, ReplicationPackageBuilder>();

    @Deactivate
    protected void deactivate() {
        replicationPackageBuilders.clear();
    }

    protected void bindReplicationPackageBuilder(
                    final ReplicationPackageBuilder replicationPackageBuilder,
                    Map<String, Object> properties) {
        synchronized (replicationPackageBuilders) {
            String name =  PropertiesUtil.toString(properties.get("name"), "");
            replicationPackageBuilders.put(name, replicationPackageBuilder);
        }
        log.debug("Registering Replication Package Builder {} ", replicationPackageBuilder);
    }

    protected void unbindReplicationPackageBuilder(
                    final ReplicationPackageBuilder replicationPackageBuilder,
                    Map<String, Object> properties) {
        synchronized (replicationPackageBuilders) {
            replicationPackageBuilders.remove(String.valueOf("name"));
        }
        log.debug("Unregistering Replication PackageBuilder {} ", replicationPackageBuilder);
    }

    public Collection<ReplicationPackageBuilder> getAvailableReplicationPackageBuilders() {
        return replicationPackageBuilders.values();
    }

    public ReplicationPackageBuilder getReplicationPackageBuilder(String name) {
        return replicationPackageBuilders.get(name);
    }

}
