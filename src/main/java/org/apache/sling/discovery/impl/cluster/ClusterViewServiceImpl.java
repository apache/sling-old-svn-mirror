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
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.cluster.UndefinedClusterViewException.Reason;
import org.apache.sling.discovery.impl.common.View;
import org.apache.sling.discovery.impl.common.ViewHelper;
import org.apache.sling.discovery.impl.common.resource.EstablishedClusterView;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the ClusterViewService interface.
 * <p>
 * This class is a reader only - it accesses the repository to read the
 * currently established view
 */
@Component
@Service(value = ClusterViewService.class)
public class ClusterViewServiceImpl implements ClusterViewService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Config config;

    public String getSlingId() {
    	if (settingsService==null) {
    		return null;
    	}
        return settingsService.getSlingId();
    }

    public boolean contains(final String slingId) throws UndefinedClusterViewException {
        List<InstanceDescription> localInstances = getClusterView().getInstances();
        for (Iterator<InstanceDescription> it = localInstances.iterator(); it
                .hasNext();) {
            InstanceDescription aLocalInstance = it.next();
            if (aLocalInstance.getSlingId().equals(slingId)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsAny(Collection<InstanceDescription> listInstances) 
            throws UndefinedClusterViewException{
        for (Iterator<InstanceDescription> it = listInstances.iterator(); it
                .hasNext();) {
            InstanceDescription instanceDescription = it.next();
            if (contains(instanceDescription.getSlingId())) {
                return true;
            }
        }
        return false;
    }

    public ClusterView getClusterView() throws UndefinedClusterViewException {
    	if (resourceResolverFactory==null) {
    		logger.warn("getClusterView: no resourceResolverFactory set at the moment.");
    		throw new UndefinedClusterViewException(Reason.REPOSITORY_EXCEPTION,
    		        "no resourceResolverFactory set");
    	}
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);

            View view = ViewHelper.getEstablishedView(resourceResolver, config);
            if (view == null) {
                logger.debug("getClusterView: no view established at the moment. isolated mode");
                throw new UndefinedClusterViewException(Reason.NO_ESTABLISHED_VIEW,
                        "no established view at the moment");
            }

            EstablishedClusterView clusterViewImpl = new EstablishedClusterView(
                    config, view, getSlingId());
            boolean foundLocal = false;
            for (Iterator<InstanceDescription> it = clusterViewImpl
                    .getInstances().iterator(); it.hasNext();) {
                InstanceDescription instance = it.next();
                if (instance.isLocal()) {
                    foundLocal = true;
                    break;
                }
            }
            if (foundLocal) {
                return clusterViewImpl;
            } else {
                logger.info("getClusterView: the local instance ("+getSlingId()+") is currently not included in the existing established view! "
                        + "This is normal at startup. At other times is pseudo-network-partitioning is an indicator for repository/network-delays or clocks-out-of-sync (SLING-3432). "
                        + "(increasing the heartbeatTimeout can help as a workaround too) "
                        + "The local instance will stay in TOPOLOGY_CHANGING or pre _INIT mode until a new vote was successful.");
                throw new UndefinedClusterViewException(Reason.ISOLATED_FROM_TOPOLOGY, 
                        "established view does not include local instance - isolated");
            }
        } catch (LoginException e) {
            logger.error(
                    "handleEvent: could not log in administratively: " + e, e);
            throw new UndefinedClusterViewException(Reason.REPOSITORY_EXCEPTION,
                    "could not log in administratively: "+e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }

    }

}
