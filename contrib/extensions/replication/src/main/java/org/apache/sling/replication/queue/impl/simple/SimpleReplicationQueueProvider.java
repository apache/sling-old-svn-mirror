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
package org.apache.sling.replication.queue.impl.simple;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.queue.impl.AbstractReplicationQueueProvider;

/**
 * an OSGi service implementing {@link ReplicationQueueProvider} for simple in memory
 * {@link ReplicationQueue}s
 */
@Component(metatype = false)
@Service(value = ReplicationQueueProvider.class)
@Property(name = "name", value = SimpleReplicationQueueProvider.NAME)
public class SimpleReplicationQueueProvider extends AbstractReplicationQueueProvider implements
                ReplicationQueueProvider {

    public static final String NAME = "simple";

    protected ReplicationQueue createQueue(ReplicationAgent agent, String selector)
                    throws ReplicationQueueException {
        return new SimpleReplicationQueue(agent);
    }

    protected void deleteQueue(ReplicationQueue queue) throws ReplicationQueueException {
        // do nothing as queues just exist in the cache
    }

}
