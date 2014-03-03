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
package org.apache.sling.discovery.impl.common.resource;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.common.DefaultClusterViewImpl;
import org.apache.sling.discovery.impl.common.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ClusterView which represents an established view and contains all members
 * as stored in the repository at the according location
 *
 */
public class EstablishedClusterView extends DefaultClusterViewImpl {

    /**
     * use static logger to avoid frequent initialization as is potentially the
     * case with ClusterViewResource.
     **/
    private final static Logger logger = LoggerFactory
            .getLogger(EstablishedClusterView.class);

    /** Construct a new established cluster view **/
    public EstablishedClusterView(final Config config, final View view,
            final String localId) {
        super(view.getViewId());

        final Resource viewRes = view.getResource();
        if (viewRes == null) {
            throw new IllegalStateException("viewRes must not be null");
        }
        final ValueMap valueMap = viewRes.adaptTo(ValueMap.class);
        if (valueMap == null) {
            throw new IllegalStateException("valueMap must not be null");
        }
        String leaderId = valueMap.get("leaderId", String.class);
        final Resource members = viewRes.getChild("members");
        if (members == null) {
            throw new IllegalStateException("members must not be null");
        }
        final Iterator<Resource> it1 = members
                .getChildren().iterator();
        final List<Resource> instanceRess = new LinkedList<Resource>();
        while (it1.hasNext()) {
            instanceRess.add(it1.next());
        }

        Collections.sort(instanceRess, new Comparator<Resource>() {
            public int compare(Resource o1, Resource o2) {
                if (o1 == o2) {
                    return 0;
                }
                if (o1 == null) {
                    return 1;
                }
                if (o2 == null) {
                    return -1;
                }
                return o1.getName().compareTo(o2.getName());
            }
        });

        if (leaderId==null || leaderId.length()==0) {
        	// fallback to pre-SLING-3253: choose leader based on slingId alone.
	        final Resource leader = instanceRess.get(0);
	        leaderId = leader.getName();
        }
        InstanceDescription leaderInstance = null;

        for (Iterator<Resource> it2 = instanceRess.iterator(); it2.hasNext();) {
            Resource resource = it2.next();
            Resource instanceResource = resource.getResourceResolver()
                    .getResource(
                            config.getClusterInstancesPath() + "/"
                                    + resource.getName());
            String slingId = resource.getName();
            EstablishedInstanceDescription instance = new EstablishedInstanceDescription(
                    this, instanceResource, slingId, slingId.equals(leaderId),
                    slingId.equals(localId));
            if (instance.isLeader()) {
                if (leaderInstance != null) {
                    logger.error("More than one instance chosen as leader! overwritten leader="
                            + leaderInstance + ", new leader=" + instance);
                }
                leaderInstance = instance;
            }
        }
    }

}
