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
package org.apache.sling.discovery.commons.providers.spi;

import org.apache.sling.discovery.commons.providers.BaseTopologyView;

/**
 * The ConsistencyService can be used to establish strong
 * consistency with the underlying (eventually consistent) repository in use.
 * <p>
 * The issue is described in length in SLING-4627 - the short
 * version is composed of two different factors:
 * <ul>
 * <li>concurrency of discovery service and its listeners on the 
 * different instances: upon a change in the topology it is 
 * important that one listener doesn't do activity based on
 * an older incarnation of the topologyView than another listener
 * on another instance. they should change from one view to the
 * next view based on the same repository state.</li>
 * </li>
 * <li>when an instance leaves the cluster (eg crashes), then 
 * depending on the repository it might have left a backlog around
 * which would yet have to be processed and which could contain
 * relevant topology-dependent data that should be waited for
 * to settle before the topology-dependent activity can continue
 * </li>
 * </ul>
 * Both of these two aspects are handled by this ConsistencyService.
 * The former one by introducing a 'sync token' that gets written
 * to the repository and on receiving it by the peers they know
 * that the writing instance is aware of the ongoing change, that
 * the writing instance has sent out TOPOLOGY_CHANGING and that
 * the receiving instance has seen all changes that the writing
 * instance did prior to sending a TOPOLOGY_CHANGING.
 * The latter aspect is achieved by making use of the underlying
 * repository: eg on Oak the 'discovery lite' descriptor is
 * used to determine if any instance not part of the new view
 * is still being deactivated (eg has backlog). So this second
 * part is repository dependent.
 */
public interface ConsistencyService {

    /**
     * Starts the synchronization process and calls the provided
     * callback upon completion.
     * <p>
     * sync() is not thread-safe and should not be invoked 
     * concurrently.
     * <p>
     * If sync() gets called before a previous invocation finished,
     * that previous invocation will be discarded, ie the callback
     * of the previous invocation will no longer be called.
     * <p>
     * The synchronization process consists of making sure that
     * the repository has processed any potential backlog of instances
     * that are no longer part of the provided, new view. Plus 
     * it writes a 'sync-token' to a well-defined location, with
     * all peers doing the same, and upon seeing all other sync-tokens
     * declares successful completion - at which point it calls the
     * callback.run().
     * @param view the view which all instances in the local cluster
     * should agree on having seen
     * @param callback the runnable which should be called after
     * successful syncing
     */
    void sync(BaseTopologyView view, Runnable callback);
    
}
