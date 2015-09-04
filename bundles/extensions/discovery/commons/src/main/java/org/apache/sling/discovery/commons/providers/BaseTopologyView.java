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

import org.apache.sling.discovery.TopologyView;

/**
 * Very simple abstract base class for the TopologyView which
 * comes with the 'setNotCurrent()' method - that allows the
 * ViewStateManager to mark a topologyView as no longer current
 * - and the isCurrent() is handled accordingly.
 */
public abstract class BaseTopologyView implements TopologyView {

    /** Whether or not this topology is considered 'current' / ie currently valid **/
    private volatile boolean current = true;
    
    /**
     * {@inheritDoc}
     */
    public boolean isCurrent() {
        return current;
    }
    
    /**
     * Marks this view as no longer current - this typically
     * results in a TOPOLOGY_CHANGING event to be sent.
     * <p>
     * Note that once marked as not current, it can no longer
     * be reverted to current==true
     */
    public void setNotCurrent() {
        current = false;
    }

}
