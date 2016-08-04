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
package org.apache.sling.discovery.commons.providers.spi.base;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.spi.ClusterSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows chaining of ClusterSyncService, itself implementing
 * the ClusterSyncService interface
 */
public class ClusterSyncServiceChain implements ClusterSyncService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<ClusterSyncService> chain;

    /**
     * Creates a new chain of ClusterSyncService that calls a
     * cascaded sync with the provided ClusterSyncService.
     */
    public ClusterSyncServiceChain(ClusterSyncService... chain) {
        if (chain==null || chain.length==0) {
            throw new IllegalArgumentException("chain must be 1 or more");
        }
        this.chain = Arrays.asList(chain);
    }
    
    @Override
    public void sync(BaseTopologyView view, Runnable callback) {
        final Iterator<ClusterSyncService> chainIt = chain.iterator();
        chainedSync(view, callback, chainIt);
    }

    private void chainedSync(final BaseTopologyView view, final Runnable callback, 
            final Iterator<ClusterSyncService> chainIt) {
        if (!chainIt.hasNext()) {
            logger.debug("doSync: done with sync chain, invoking callback");
            callback.run();
            return;
        }
        ClusterSyncService next = chainIt.next();
        next.sync(view, new Runnable() {

            @Override
            public void run() {
                chainedSync(view, callback, chainIt);
            }
            
        });
    }

    @Override
    public void cancelSync() {
        for (ClusterSyncService consistencyService : chain) {
            consistencyService.cancelSync();
        }
    }

}
