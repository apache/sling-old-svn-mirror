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
package org.apache.sling.discovery.impl.common;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.impl.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** helper for views **/
public class ViewHelper {

    private static final Logger logger = LoggerFactory
            .getLogger(ViewHelper.class);

    /**
     * Return the list of cluster instances that are 'live', ie that have
     * sent a heartbeat within the configured heartbeat timeout
     * @param clusterInstancesResource
     * @param config
     * @return
     */
    public static Set<String> determineLiveInstances(
            final Resource clusterInstancesResource, final Config config) {
        final Set<String> myView = new HashSet<String>();
        final Iterator<Resource> it = clusterInstancesResource.getChildren()
                .iterator();
        while (it.hasNext()) {
            Resource aClusterInstance = it.next();
            if (isHeartBeatCurrent(aClusterInstance, config)) {
                myView.add(aClusterInstance.getName());
            }
        }
        return myView;
    }

    /**
     * Chck if the given resource has a heartbeat sent within the
     * configured heartbeat timeout
     * @param aClusterInstanceResource
     * @param config
     * @return
     */
    private static boolean isHeartBeatCurrent(
            Resource aClusterInstanceResource, final Config config) {
        final ValueMap properties = aClusterInstanceResource.adaptTo(ValueMap.class);
        final Date lastHeartbeat = properties.get("lastHeartbeat", Date.class);
        final long now = System.currentTimeMillis();
        if (lastHeartbeat == null) {
            return false;
        }
        final long then = lastHeartbeat.getTime();
        final long diff = now - then;
        return (diff < 1000 * config.getHeartbeatTimeout());
    }

    /**
     * Return the currently established cluster view - or null if there is no
     * cluster view established at the moment.
     * 
     * @param resourceResolver
     * @return
     */
    public static View getEstablishedView(final ResourceResolver resourceResolver, final Config config) {
        final Resource establishedParent = resourceResolver
                .getResource(config.getEstablishedViewPath());
        if (establishedParent == null) {
            return null;
        }
        final Iterable<Resource> children = establishedParent.getChildren();
        if (children == null) {
            return null;
        }
        final Iterator<Resource> it = children.iterator();
        if (!it.hasNext()) {
            return null;
        }
        Resource establishedView = it.next();
        if (!it.hasNext()) {
            return new View(establishedView);
        }
        // emergency cleanup in case there is more than one established view:
        while (true) {
            logger.error("getEstablishedView: more than one established view encountered! Removing: "
                    + establishedView);
            new View(establishedView).remove();
            if (!it.hasNext()) {
                return null;
            }
            establishedView = it.next();
        }
    }

    /**
     * Check if the established view matches the given set of slingIds
     */
    public static boolean establishedViewMatches(
            final ResourceResolver resourceResolver, final Config config, final Set<String> view) {
        final View establishedView = ViewHelper.getEstablishedView(resourceResolver, config);
        if (establishedView == null) {
            return false;
        } else {
            return (establishedView.matches(view));
        }
    }

}
