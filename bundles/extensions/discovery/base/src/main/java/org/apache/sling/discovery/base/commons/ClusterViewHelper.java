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

import java.util.Collection;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

/**
 * Contains some static helper methods around ClusterView
 */
public class ClusterViewHelper {

    /** checks whether the cluster view contains a particular sling id **/
    public static boolean contains(ClusterView clusterView, String slingId) throws UndefinedClusterViewException {
        InstanceDescription found = null;
        for (InstanceDescription i : clusterView.getInstances()) {
            if (i.getSlingId().equals(slingId)) {
                if (found!=null) {
                    throw new IllegalStateException("multiple instances with slingId found: "+slingId);
                }
                found = i;
            }
        }
        return found!=null;
    }

    /** checks whether the cluster contains any of the provided instances **/
    public static boolean containsAny(ClusterView clusterView, Collection<InstanceDescription> listInstances) 
            throws UndefinedClusterViewException {
        for (InstanceDescription i : listInstances) {
            if (contains(clusterView, i.getSlingId())) {
                return true;
            }
        }
        return false;
    }
}
