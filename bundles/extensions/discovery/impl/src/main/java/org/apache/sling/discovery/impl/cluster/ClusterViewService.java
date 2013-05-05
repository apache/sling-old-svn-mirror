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
package org.apache.sling.discovery.impl.cluster;

import java.util.Collection;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

/**
 * The ClusterViewService is responsible and provides access to the view
 * established in a JcR cluster.
 */
public interface ClusterViewService {

    /** the sling id of the local instance **/
    String getSlingId();

    /** the current cluster view **/
    ClusterView getClusterView();

    /**
     * the view id of the cluster view when isolated - ie before any view is
     * established
     **/
    String getIsolatedClusterViewId();

    /** checks whether the cluster view contains a particular sling id **/
    boolean contains(String slingId);

    /** checks whether the cluster contains any of the provided instances **/
    boolean containsAny(Collection<InstanceDescription> listInstances);

}
