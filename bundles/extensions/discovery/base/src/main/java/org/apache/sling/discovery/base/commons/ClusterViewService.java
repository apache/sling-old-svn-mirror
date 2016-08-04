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
package org.apache.sling.discovery.base.commons;

import org.apache.sling.discovery.commons.providers.spi.LocalClusterView;

/**
 * The ClusterViewService is capable of determining the 
 * ClusterView of the local cluster (ie of the instances
 * that are all hooked to the same underlying repository).
 */
public interface ClusterViewService {

    /** the sling id of the local instance **/
    String getSlingId();

    /**
     * Returns the current, local cluster view - throwing an
     * UndefinedClusterViewException if it cannot determine
     * a clusterView at the moment.
     * @return the current cluster view - never returns null 
     * (it rather throws an UndefinedClusterViewException that
     * contains more details about why exactly the clusterView
     * is undefined at the moment)
     * @throws UndefinedClusterViewException thrown when
     * the ClusterView cannot be determined at the moment
     * (also contains more details as to why exactly)
     */
    LocalClusterView getLocalClusterView() throws UndefinedClusterViewException;

}
