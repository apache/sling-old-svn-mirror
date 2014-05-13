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
package org.apache.sling.replication.agent.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentsManager;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ReplicationAgentsManager}
 */
@Component
@References({
        @Reference(name = "replicationAgent",
                referenceInterface = ReplicationAgent.class,
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
                policy = ReferencePolicy.DYNAMIC,
                bind = "bindReplicationAgent",
                unbind = "unbindReplicationAgent")
})
@Service(value = ReplicationAgentsManager.class)
public class DefaultReplicationAgentsManager implements ReplicationAgentsManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SortedSet<ReplicationAgent> replicationAgents = new TreeSet<ReplicationAgent>(new ReplicationAgentComparator());

    public SortedSet<ReplicationAgent> getAgentsFor(ReplicationActionType action, String... paths) {
        // TODO : implement the filtering based on rules here
        return Collections.unmodifiableSortedSet(replicationAgents);
    }

    public Collection<ReplicationAgent> getAllAvailableAgents() {
        return Collections.unmodifiableCollection(replicationAgents);
    }

    @Deactivate
    protected void deactivate() {
        replicationAgents.clear();
    }

    protected void bindReplicationAgent(final ReplicationAgent replicationAgent,
                                        Map<String, Object> properties) {
        synchronized (replicationAgents) {
            replicationAgents.add(replicationAgent);
        }
        log.debug("Registering replication agent {} ", replicationAgent);
    }

    protected void unbindReplicationAgent(final ReplicationAgent replicationAgent,
                                          Map<String, Object> properties) {
        synchronized (replicationAgents) {
            replicationAgents.remove(replicationAgent);
        }
        log.debug("Unregistering replication agent {} ", replicationAgent);
    }

    private static final class ReplicationAgentComparator implements Comparator<ReplicationAgent> {
        public int compare(ReplicationAgent o1, ReplicationAgent o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
