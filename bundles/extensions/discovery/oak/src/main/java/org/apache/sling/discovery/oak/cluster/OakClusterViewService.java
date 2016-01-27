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
package org.apache.sling.discovery.oak.cluster;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.commons.UndefinedClusterViewException;
import org.apache.sling.discovery.base.commons.UndefinedClusterViewException.Reason;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescription;
import org.apache.sling.discovery.commons.providers.spi.LocalClusterView;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteDescriptor;
import org.apache.sling.discovery.commons.providers.spi.base.IdMapService;
import org.apache.sling.discovery.commons.providers.util.ResourceHelper;
import org.apache.sling.discovery.oak.Config;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Oak-based implementation of the ClusterViewService interface.
 */
@Component
@Service(value = ClusterViewService.class)
public class OakClusterViewService implements ClusterViewService {
    
    private static final String PROPERTY_CLUSTER_ID = "clusterId";
    private static final String PROPERTY_CLUSTER_ID_DEFINED_AT = "clusterIdDefinedAt";
    private static final String PROPERTY_CLUSTER_ID_DEFINED_BY = "clusterIdDefinedBy";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Config config;
    
    @Reference
    private IdMapService idMapService;
    
    /** the last sequence number read from the oak discovery-lite descriptor **/
    private long lastSeqNum = -1;
    
    public static OakClusterViewService testConstructor(SlingSettingsService settingsService,
            ResourceResolverFactory resourceResolverFactory,
            IdMapService idMapService,
            Config config) {
        OakClusterViewService service = new OakClusterViewService();
        service.settingsService = settingsService;
        service.resourceResolverFactory = resourceResolverFactory;
        service.config = config;
        service.idMapService = idMapService;
        return service;
    }
    
    public String getSlingId() {
    	if (settingsService==null) {
    		return null;
    	}
        return settingsService.getSlingId();
    }

    protected ResourceResolver getResourceResolver() throws LoginException {
        return resourceResolverFactory.getAdministrativeResourceResolver(null);
    }

    public LocalClusterView getLocalClusterView() throws UndefinedClusterViewException {
        logger.trace("getLocalClusterView: start");
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            DiscoveryLiteDescriptor descriptor = 
                    DiscoveryLiteDescriptor.getDescriptorFrom(resourceResolver);
            if (lastSeqNum!=descriptor.getSeqNum()) {
                logger.info("getLocalClusterView: sequence number change detected - clearing idmap cache");
                idMapService.clearCache();
                lastSeqNum = descriptor.getSeqNum();
            }
            return asClusterView(descriptor, resourceResolver);
        } catch (UndefinedClusterViewException e) {
            logger.info("getLocalClusterView: undefined clusterView: "+e.getReason()+" - "+e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("getLocalClusterView: repository exception: "+e, e);
            throw new UndefinedClusterViewException(Reason.REPOSITORY_EXCEPTION, "Exception while processing descriptor: "+e);
        } finally {
            logger.trace("getLocalClusterView: end");
            if (resourceResolver!=null) {
                resourceResolver.close();
            }
        }
    }

    private LocalClusterView asClusterView(DiscoveryLiteDescriptor descriptor, ResourceResolver resourceResolver) throws Exception {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (resourceResolver==null) {
            throw new IllegalArgumentException("resourceResolver must not be null");
        }
        logger.trace("asClusterView: start");
        String clusterViewId = descriptor.getViewId();
        if (clusterViewId == null || clusterViewId.length() == 0) {
            logger.trace("asClusterView: no clusterId provided by discovery-lite descriptor - reading from repo.");
            clusterViewId = readOrDefineClusterId(resourceResolver);
        }
        String localClusterSyncTokenId = /*descriptor.getViewId()+"_"+*/String.valueOf(descriptor.getSeqNum());
        if (!descriptor.isFinal()) {
            throw new UndefinedClusterViewException(Reason.NO_ESTABLISHED_VIEW, "descriptor is not yet final: "+descriptor);
        }
        LocalClusterView cluster = new LocalClusterView(clusterViewId, localClusterSyncTokenId);
        long me = descriptor.getMyId();
        int[] activeIds = descriptor.getActiveIds();
        if (activeIds==null || activeIds.length==0) {
            throw new UndefinedClusterViewException(Reason.NO_ESTABLISHED_VIEW, "Descriptor contained no active ids: "+descriptor.getDescriptorStr());
        }
        // convert int[] to List<Integer>
        //TODO: could use Guava's Ints class here..
        List<Integer> activeIdsList = new LinkedList<Integer>();
        for (Integer integer : activeIds) {
            activeIdsList.add(integer);
        }
        
        // step 1: sort activeIds by their leaderElectionId
        //   serves two purposes: pos[0] is then leader
        //   and the rest are properly sorted within the cluster
        final Map<Integer, String> leaderElectionIds = new HashMap<Integer, String>();
        for (Integer id : activeIdsList) {
            String slingId = idMapService.toSlingId(id, resourceResolver);
            if (slingId == null) {
                idMapService.clearCache();
                throw new UndefinedClusterViewException(Reason.NO_ESTABLISHED_VIEW,
                        "no slingId mapped for clusterNodeId="+id);
            }
            String leaderElectionId = getLeaderElectionId(resourceResolver,
                    slingId);
            leaderElectionIds.put(id, leaderElectionId);
        }
        
        Collections.sort(activeIdsList, new Comparator<Integer>() {

            @Override
            public int compare(Integer arg0, Integer arg1) {
                return leaderElectionIds.get(arg0)
                        .compareTo(leaderElectionIds.get(arg1));
            }
        });
        
        for(int i=0; i<activeIdsList.size(); i++) {
            int id = activeIdsList.get(i);
            boolean isLeader = i==0; // thx to sorting above [0] is leader indeed
            boolean isOwn = id==me;
            String slingId = idMapService.toSlingId(id, resourceResolver);
            if (slingId==null) {
                idMapService.clearCache();
                logger.info("asClusterView: cannot resolve oak-clusterNodeId {} to a slingId", id);
                throw new Exception("Cannot resolve oak-clusterNodeId "+id+" to a slingId");
            }
            Map<String, String> properties = readProperties(slingId, resourceResolver);
            // create a new instance (adds itself to the cluster in the constructor)
            new DefaultInstanceDescription(cluster, isLeader, isOwn, slingId, properties);
        }
        logger.trace("asClusterView: returning {}", cluster);
        InstanceDescription local = cluster.getLocalInstance();
        if (local != null) {
            return cluster;
        } else {
            logger.info("getClusterView: the local instance ("+getSlingId()+") is currently not included in the existing established view! "
                    + "This is normal at startup. At other times is pseudo-network-partitioning is an indicator for repository/network-delays or clocks-out-of-sync (SLING-3432). "
                    + "(increasing the heartbeatTimeout can help as a workaround too) "
                    + "The local instance will stay in TOPOLOGY_CHANGING or pre _INIT mode until a new vote was successful.");
            throw new UndefinedClusterViewException(Reason.ISOLATED_FROM_TOPOLOGY, 
                    "established view does not include local instance - isolated");
        }
    }

    /**
     * oak's discovery-lite can opt to not provide a clusterViewId eg in the
     * single-VM case. (for clusters discovery-lite normally defines the
     * clusterViewId, as it is the one responsible for defining the membership
     * too) Thus if we're not getting an id here we have to define one here. (we
     * can typically assume that this corresponds to a singleVM case, but that's
     * not a 100% requirement). This id must be stored to ensure the contract
     * that the clusterId is stable across restarts. For that, the id is stored
     * under /var/discovery/oak (and to account for odd/edgy cases we'll do a
     * retry when storing the id, in case we'd run into conflicts, even though
     * they should not occur in singleVM cases)
     * 
     * @param resourceResolver the ResourceResolver with which to read or write
     * the clusterId properties under /var/discovery/oak
     * @return the clusterId to be used - either the one read or defined
     * at /var/discovery/oak - or the slingId in case of non-fixable exceptions
     * @throws PersistenceException when /var/discovery/oak could not be
     * accessed or auto-created
     */
    private String readOrDefineClusterId(ResourceResolver resourceResolver) throws PersistenceException {
        //TODO: if Config gets a specific, public getDiscoveryResourcePath, this can be simplified:
        final String clusterInstancesPath = config.getClusterInstancesPath();
        final String discoveryResourcePath = clusterInstancesPath.substring(0, 
                clusterInstancesPath.lastIndexOf("/", clusterInstancesPath.length()-2));
        final int MAX_RETRIES = 5;
        for(int retryCnt=0; retryCnt<MAX_RETRIES; retryCnt++) {
            Resource varDiscoveryOak = resourceResolver.getResource(discoveryResourcePath);
            if (varDiscoveryOak == null) {
                varDiscoveryOak = ResourceHelper.getOrCreateResource(resourceResolver, discoveryResourcePath);
            }
            if (varDiscoveryOak == null) {
                logger.error("readOrDefinedClusterId: Could not create: "+discoveryResourcePath);
                throw new RuntimeException("could not create " + discoveryResourcePath);
            }
            ModifiableValueMap props = varDiscoveryOak.adaptTo(ModifiableValueMap.class);
            if (props == null) {
                logger.error("readOrDefineClusterId: Could not adaptTo ModifiableValueMap: "+varDiscoveryOak);
                throw new RuntimeException("could not adaptTo ModifiableValueMap: " + varDiscoveryOak);
            }
            Object clusterIdObj = props.get(PROPERTY_CLUSTER_ID);
            String clusterId = (clusterIdObj == null) ? null : String.valueOf(clusterIdObj);
            if (clusterId != null && clusterId.length() > 0) {
                logger.trace("readOrDefineClusterId: read clusterId from repo as {}", clusterId);
                return clusterId;
            }

            // must now define a new clusterId and store it under /var/discovery/oak
            final String newClusterId = UUID.randomUUID().toString();
            props.put(PROPERTY_CLUSTER_ID, newClusterId);
            props.put(PROPERTY_CLUSTER_ID_DEFINED_BY, getSlingId());
            props.put(PROPERTY_CLUSTER_ID_DEFINED_AT, Calendar.getInstance());
            try {
                logger.info("readOrDefineClusterId: storing new clusterId as " + newClusterId);
                resourceResolver.commit();
                return newClusterId;
            } catch (PersistenceException e) {
                logger.warn("readOrDefineClusterId: could not persist clusterId "
                        + "(retrying in 1 sec max " + (MAX_RETRIES - retryCnt - 1) + " more times: " + e, e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    logger.warn("readOrDefineClusterId: got interrupted: "+e1, e1);
                }
                logger.info("readOrDefineClusterId: retrying now.");
            }
        }
        throw new RuntimeException("failed to write new clusterId (see log file earlier for more details)");
    }

    private String getLeaderElectionId(ResourceResolver resourceResolver, String slingId) {
        if (slingId==null) {
            throw new IllegalStateException("slingId must not be null");
        }
        final String myClusterNodePath = config.getClusterInstancesPath()+"/"+slingId;
        ValueMap resourceMap = resourceResolver.getResource(myClusterNodePath)
                .adaptTo(ValueMap.class);
        String result = resourceMap.get("leaderElectionId", String.class);
        return result;
    }
    
    private Map<String, String> readProperties(String slingId, ResourceResolver resourceResolver) {
        Resource res = resourceResolver.getResource(
                        config.getClusterInstancesPath() + "/"
                                + slingId);
        final Map<String, String> props = new HashMap<String, String>();
        if (res != null) {
            final Resource propertiesChild = res.getChild("properties");
            if (propertiesChild != null) {
                final ValueMap properties = propertiesChild.adaptTo(ValueMap.class);
                if (properties != null) {
                    for (Iterator<String> it = properties.keySet().iterator(); it
                            .hasNext();) {
                        String key = it.next();
                        if (!key.equals("jcr:primaryType")) {
                            props.put(key, properties.get(key, String.class));
                        }
                    }
                }
            }
        }
        return props;
    }

}
