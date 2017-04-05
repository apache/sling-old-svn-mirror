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
package org.apache.sling.discovery.commons.providers;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.junit.Test;

public class BaseTopologyViewTest {

    BaseTopologyView newView() {
        return new BaseTopologyView() {
            
            @Override
            public InstanceDescription getLocalInstance() {
                throw new IllegalStateException("not yet impl");
            }
            
            @Override
            public Set<InstanceDescription> getInstances() {
                throw new IllegalStateException("not yet impl");
            }
            
            @Override
            public Set<ClusterView> getClusterViews() {
                throw new IllegalStateException("not yet impl");
            }
            
            @Override
            public Set<InstanceDescription> findInstances(InstanceFilter filter) {
                throw new IllegalStateException("not yet impl");
            }
            
            @Override
            public String getLocalClusterSyncTokenId() {
                throw new IllegalStateException("not yet impl");
            }
        };
    }
    
    @Test
    public void testCurrent() throws Exception {
        BaseTopologyView view = newView();
        assertTrue(view.isCurrent());
        view.setNotCurrent();
        assertTrue(!view.isCurrent());
    }
}
