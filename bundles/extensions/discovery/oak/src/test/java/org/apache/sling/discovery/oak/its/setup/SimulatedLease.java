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

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.commons.providers.spi.base.DescriptorHelper;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteDescriptorBuilder;

public class SimulatedLease {

    private final SimulatedLeaseCollection collection;
    private final ResourceResolverFactory factory;
    private final String slingId;

    public SimulatedLease(ResourceResolverFactory factory,
            SimulatedLeaseCollection collection,
            String slingId) {
        this.factory = factory;
        this.collection = collection;
        collection.hooked(this);
        this.slingId = slingId;
    }
    
    @Override
    public String toString() {
        return "a SimulatedLease[slingId="+slingId+"]";
    }
    
    public String getSlingId() {
        return slingId;
    }

    public void updateDescriptor(OakTestConfig config) throws Exception {
        DiscoveryLiteDescriptorBuilder builder = collection.getDescriptorFor(this, config);
        DescriptorHelper.setDiscoveryLiteDescriptor(factory, builder);
    }
    
    public void updateLeaseAndDescriptor(OakTestConfig config) throws Exception {
        DiscoveryLiteDescriptorBuilder builder = collection.updateAndGetDescriptorFor(this, config);
        DescriptorHelper.setDiscoveryLiteDescriptor(factory, builder);
    }

}
