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
package org.apache.sling.discovery.oak.its.setup;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteDescriptorBuilder;

public class SimulatedLeaseCollection {

    private final Map<String,Long> leaseUpdates = 
            new HashMap<String, Long>();
    
    private final Map<String,Integer> clusterNodeIds =
            new HashMap<String, Integer>();
    
    private int highestId = 0;
    
    private final String viewId = UUID.randomUUID().toString();
    
    List<SimulatedLease> leases = new LinkedList<SimulatedLease>();

    private volatile boolean isFinal = true;

    private int seqNum = 0;
    
    public SimulatedLeaseCollection() {
        // empty
    }
    
    public int getSeqNum() {
        return seqNum;
    }
    
    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }
    
    public void incSeqNum() {
        seqNum++;
    }
    
    public void incSeqNum(int amount) {
        if (amount<=0) {
            throw new IllegalArgumentException("amount must be >0, is: "+amount);
        }
        seqNum+=amount;
    }

    public synchronized void hooked(SimulatedLease lease) {
        leases.add(lease);
    }

    public synchronized DiscoveryLiteDescriptorBuilder getDescriptorFor(SimulatedLease simulatedLease, OakTestConfig config) {
        return doUpdateAndGet(simulatedLease, config, false);
    }
    
    public synchronized DiscoveryLiteDescriptorBuilder updateAndGetDescriptorFor(SimulatedLease simulatedLease, OakTestConfig config) {
        return doUpdateAndGet(simulatedLease, config, true);
    }

    private DiscoveryLiteDescriptorBuilder doUpdateAndGet(SimulatedLease simulatedLease, OakTestConfig config, boolean updateLease) {
        int clusterNodeId = getClusterNodeId(simulatedLease.getSlingId());
        if (updateLease) {
            leaseUpdates.put(simulatedLease.getSlingId(), System.currentTimeMillis());
        }
        DiscoveryLiteDescriptorBuilder discoBuilder = 
                new DiscoveryLiteDescriptorBuilder();
        discoBuilder.me(clusterNodeId);
        discoBuilder.id(viewId);
        discoBuilder.setFinal(isFinal);
        discoBuilder.seq(seqNum);
        List<Integer> actives = new LinkedList<Integer>();
        List<Integer> inactives = new LinkedList<Integer>();
        for (Map.Entry<String, Long> entry : leaseUpdates.entrySet()) {
            int id = getClusterNodeId(entry.getKey());
            if (isTimedout(entry.getValue(), config)) {
                inactives.add(id);
            } else {
                actives.add(id);
            }
        }
        discoBuilder.activeIds(actives.toArray(new Integer[0]));
        discoBuilder.inactiveIds(inactives.toArray(new Integer[0]));
        return discoBuilder;
    }

    private boolean isTimedout(Long lastHeartbeat, OakTestConfig config) {
        return System.currentTimeMillis() > lastHeartbeat + (1000 * config.getViewCheckerTimeout());
    }

    private int getClusterNodeId(String slingId) {
        Integer id = clusterNodeIds.get(slingId);
        if (id==null) {
            id = ++highestId;
            clusterNodeIds.put(slingId, id);
        }
        return id;
    }

    public void reset() {
        clusterNodeIds.clear();
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

}
