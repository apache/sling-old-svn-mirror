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
package org.apache.sling.discovery.impl.topology;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * osgi event handler which takes note when the established view changes in the
 * repository - or when an announcement changed in one of the instances
 */
@Component(immediate = true)
@Service(value = EventHandler.class)
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Cluster View Change Listener 2."),
        @Property(name = EventConstants.EVENT_TOPIC, value = {
                SlingConstants.TOPIC_RESOURCE_ADDED,
                SlingConstants.TOPIC_RESOURCE_CHANGED,
                SlingConstants.TOPIC_RESOURCE_REMOVED }) })
// ,
// @Property(name = EventConstants.EVENT_FILTER, value = "(path="
// + org.apache.sling.discovery.viewmgr.Constants.ROOT_PATH + ")") })
public class TopologyChangeHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private SlingSettingsService slingSettingsService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private DiscoveryServiceImpl discoveryService;
    
    @Reference
    private Config config;

    /** the sling id of the local instance **/
    private String slingId;

    protected void activate(final ComponentContext context) {
        slingId = slingSettingsService.getSlingId();
    	if (logger.isDebugEnabled()) {
	        logger.debug("activated. slingid=" + slingId + ", discoveryservice="
	                + discoveryService);
    	}
    }

    /**
     * Handle osgi events from the repository and take note when
     * the established view, properties or announcements change - and
     * inform the DiscoveryServiceImpl in those cases.
     */
    public void handleEvent(final Event event) {
        final String resourcePath = (String) event.getProperty("path");
        if (config==null) {
            return;
        }
        final String establishedViewPath = config.getEstablishedViewPath();
        final String clusterInstancesPath = config.getClusterInstancesPath();
        if (resourcePath == null) {
            // not of my business
            return;
        }

        // properties: path, resourceChangedAttributes, resourceType,
        // event.topics
        if (resourcePath.startsWith(establishedViewPath)) {
        	if (logger.isDebugEnabled()) {
	            logger.debug("handleEvent: establishedViewPath resourcePath="
	                    + resourcePath + ", event=" + event);
        	}
            handleTopologyChanged();
        } else if (resourcePath.startsWith(clusterInstancesPath)) {

            final Object resourceChangedAttributes = event
                    .getProperty("resourceChangedAttributes");
            if (resourceChangedAttributes != null
                    && resourceChangedAttributes instanceof String[]) {
                String[] resourceChangedAttributesStrings = (String[]) resourceChangedAttributes;
                if (resourceChangedAttributesStrings.length == 1
                        && resourceChangedAttributesStrings[0]
                                .equals("lastHeartbeat")) {
                    // then ignore this one
                    return;
                }
            }
        	if (logger.isDebugEnabled()) {
	            logger.debug("handleEvent: clusterInstancesPath (announcement or properties) resourcePath="
	                    + resourcePath + ", event=" + event);
        	}
            handleTopologyChanged();
        } else {
            // not of my business
            return;
        }

    }

    /** Inform the DiscoveryServiceImpl that the topology (might) have changed **/
    private void handleTopologyChanged() {
        discoveryService.handlePotentialTopologyChange();
    }

}