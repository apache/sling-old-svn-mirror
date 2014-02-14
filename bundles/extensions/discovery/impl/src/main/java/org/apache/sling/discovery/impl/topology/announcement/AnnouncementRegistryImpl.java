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
package org.apache.sling.discovery.impl.topology.announcement;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.common.resource.ResourceHelper;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(value = AnnouncementRegistry.class)
public class AnnouncementRegistryImpl implements AnnouncementRegistry {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private Config config;
    
    private final Map<String,CachedAnnouncement> ownAnnouncementsCache = 
            new HashMap<String,CachedAnnouncement>();

    public synchronized void unregisterAnnouncement(final String ownerId) {
        if (ownerId==null || ownerId.length()==0) {
            throw new IllegalArgumentException("ownerId must not be null or empty");
        }
        // remove from the cache - even if there's an error afterwards
        ownAnnouncementsCache.remove(ownerId);
        
        if (resourceResolverFactory == null) {
            logger.error("unregisterAnnouncement: resourceResolverFactory is null");
            return;
        }
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);

            final String path = config.getClusterInstancesPath()
                    + "/"
                    + settingsService.getSlingId()
                    + "/announcements/" + ownerId;
            final Resource announcementsResource = resourceResolver.getResource(path);
            if (announcementsResource!=null) {
                resourceResolver.delete(announcementsResource);
                resourceResolver.commit();
            }

        } catch (LoginException e) {
            logger.error(
                    "unregisterAnnouncement: could not log in administratively: "
                            + e, e);
            throw new RuntimeException("Could not log in to repository (" + e
                    + ")", e);
        } catch (PersistenceException e) {
            logger.error("unregisterAnnouncement: got a PersistenceException: "
                    + e, e);
            throw new RuntimeException(
                    "Exception while talking to repository (" + e + ")", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    public synchronized Collection<Announcement> listLocalAnnouncements() {
        return fillWithCachedAnnouncements(new LinkedList<Announcement>());
    }
    
    public synchronized Collection<CachedAnnouncement> listLocalIncomingAnnouncements() {
        Collection<CachedAnnouncement> result = new LinkedList<CachedAnnouncement>(ownAnnouncementsCache.values());
        for (Iterator<CachedAnnouncement> it = result.iterator(); it.hasNext();) {
            CachedAnnouncement cachedAnnouncement = it.next();
            if (cachedAnnouncement.getAnnouncement().isInherited()) {
                it.remove();
            }
            if (System.currentTimeMillis() > cachedAnnouncement.getLastHeartbeat() + config.getHeartbeatTimeoutMillis()) {
                it.remove();
            }
        }
        return result;
    }
    
    private final InstanceDescription getLocalInstanceDescription(final ClusterView localClusterView) {
        for (Iterator<InstanceDescription> it = localClusterView.getInstances().iterator(); it
                .hasNext();) {
            InstanceDescription id = it.next();
            if (id.isLocal()) {
                return id;
            }
        }
        return null;
    }

    public synchronized Collection<Announcement> listAnnouncementsInSameCluster(final ClusterView localClusterView) {
        if (localClusterView==null) {
            throw new IllegalArgumentException("clusterView must not be null");
        }
        ResourceResolver resourceResolver = null;
        final Collection<Announcement> incomingAnnouncements = new LinkedList<Announcement>();
        final InstanceDescription localInstance = getLocalInstanceDescription(localClusterView);
        try {
            resourceResolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);

            Resource clusterInstancesResource = ResourceHelper
                    .getOrCreateResource(
                            resourceResolver,
                            config.getClusterInstancesPath());

            Iterator<Resource> it0 = clusterInstancesResource.getChildren()
                    .iterator();
            while (it0.hasNext()) {
                Resource aClusterInstanceResource = it0.next();
                final String instanceId = aClusterInstanceResource.getName();
                if (localInstance!=null && localInstance.getSlingId().equals(instanceId)) {
                    // this is the local instance then - which we serve from the cache only
                    fillWithCachedAnnouncements(incomingAnnouncements);
                    continue;
                }
                
                //TODO: add ClusterView.contains(instanceSlingId) for convenience to next api change
                if (!contains(localClusterView, instanceId)) {
                    // then the instance is not in my view, hence ignore its announcements
                    // (corresponds to earlier expiry-handling)
                    continue;
                }
                final Resource announcementsResource = aClusterInstanceResource
                        .getChild("announcements");
                if (announcementsResource == null) {
                    continue;
                }
                Iterator<Resource> it = announcementsResource.getChildren()
                        .iterator();
                Announcement topologyAnnouncement;
                while (it.hasNext()) {
                    Resource anAnnouncement = it.next();
                    topologyAnnouncement = Announcement
                            .fromJSON(anAnnouncement
                                    .adaptTo(ValueMap.class).get(
                                            "topologyAnnouncement",
                                            String.class));
                    incomingAnnouncements.add(topologyAnnouncement);
                    // SLING-3389: no longer check for expired announcements - 
                    // instead make use of the fact that this instance
                    // has a clusterView and that every live instance
                    // is responsible of cleaning up expired announcements
                    // with the repository
                }
            }

            resourceResolver.commit();
        } catch (LoginException e) {
            logger.error(
                    "listAnnouncementsInSameCluster: could not log in administratively: " + e, e);
            throw new RuntimeException("Could not log in to repository (" + e
                    + ")", e);
        } catch (PersistenceException e) {
            logger.error("listAnnouncementsInSameCluster: got a PersistenceException: " + e, e);
            throw new RuntimeException(
                    "Exception while talking to repository (" + e + ")", e);
        } catch (JSONException e) {
            logger.error("listAnnouncementsInSameCluster: got a JSONException: " + e, e);
            throw new RuntimeException("Exception while converting json (" + e
                    + ")", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    	if (logger.isDebugEnabled()) {
    		logger.debug("listAnnouncementsInSameCluster: result: "+incomingAnnouncements.size());
    	}
        return incomingAnnouncements;
    }
    
    private final Collection<Announcement> fillWithCachedAnnouncements(
            final Collection<Announcement> incomingAnnouncements) {
        for (Iterator<Entry<String, CachedAnnouncement>> it = ownAnnouncementsCache.entrySet().iterator(); it
                .hasNext();) {
            final Entry<String, CachedAnnouncement> entry = it.next();
            if (System.currentTimeMillis() > entry.getValue().getLastHeartbeat() + config.getHeartbeatTimeoutMillis()) {
                // filter this one out then
                continue;
            }
            incomingAnnouncements.add(entry.getValue().getAnnouncement());
        }
        return incomingAnnouncements;
    }

    private final boolean contains(final ClusterView clusterView, final String instanceId) {
        for (Iterator<InstanceDescription> it = clusterView.getInstances().iterator(); it
                .hasNext();) {
            InstanceDescription instance = it.next();
            if (instance.getSlingId().equals(instanceId)) {
                // fine, then the instance is in the view
                return true;
            }
        }
        return false;
    }

    public synchronized boolean hasActiveAnnouncement(final String ownerId) {
        if (ownerId==null || ownerId.length()==0) {
            throw new IllegalArgumentException("ownerId must not be null or empty: "+ownerId);
        }
        final CachedAnnouncement cachedAnnouncement = ownAnnouncementsCache.get(ownerId);
        if (cachedAnnouncement==null) {
            return false;
        }
        
        return !(System.currentTimeMillis() > cachedAnnouncement.getLastHeartbeat() + config.getHeartbeatTimeoutMillis());
    }

    public synchronized boolean registerAnnouncement(final Announcement topologyAnnouncement) {
        if (topologyAnnouncement==null) {
            throw new IllegalArgumentException("topologyAnnouncement must not be null");
        }
        if (!topologyAnnouncement.isValid()) {
            logger.warn("topologyAnnouncement is not valid");
            return false;
        }
        if (resourceResolverFactory == null) {
            logger.error("registerAnnouncement: resourceResolverFactory is null");
            return false;
        }
        
        final CachedAnnouncement cachedAnnouncement = 
                ownAnnouncementsCache.get(topologyAnnouncement.getOwnerId());
        if (cachedAnnouncement!=null) {
            if (logger.isDebugEnabled()) {
                logger.debug("registerAnnouncement: got existing cached announcement for ownerId="+topologyAnnouncement.getOwnerId());
            }
            try{
                if (topologyAnnouncement.equalsIgnoreCreated(cachedAnnouncement.getAnnouncement())) {
                    // then nothing has changed with this announcement, so just update
                    // the heartbeat and fine is.
                    // this should actually be the normal case for a stable connector
                    logger.debug("registerAnnouncement: nothing has changed, only updating heartbeat in-memory.");
                    cachedAnnouncement.registerHeartbeat();
                    return true;
                }
                
                logger.debug("registerAnnouncement: incoming announcement differs existing one!");
                
            } catch(JSONException e) {
                logger.error("registerAnnouncement: got JSONException while converting incoming announcement to JSON: "+e, e);
            }
            // otherwise the repository and the cache require to be updated
            // resetting the cache therefore at this point already
            ownAnnouncementsCache.remove(topologyAnnouncement.getOwnerId());
        } else {
            logger.debug("registerAnnouncement: no cached announcement yet for ownerId="+topologyAnnouncement.getOwnerId());
        }

        logger.debug("registerAnnouncement: getting the list of all local announcements");
        final Collection<Announcement> announcements = new LinkedList<Announcement>();
        fillWithCachedAnnouncements(announcements);
        if (logger.isDebugEnabled()) {
            logger.debug("registerAnnouncement: list returned: "+(announcements==null ? "null" : announcements.size()));
        }
        for (Iterator<Announcement> it1 = announcements.iterator(); it1
                .hasNext();) {
            Announcement announcement = it1.next();
            if (announcement.getOwnerId().equals(
                    topologyAnnouncement.getOwnerId())) {
                // then this is from the same owner - skip this
                continue;
            }
            // analyse to see if any of the instances in the announcement
            // include the new owner
            Collection<InstanceDescription> attachedInstances = announcement
                    .listInstances();
            for (Iterator<InstanceDescription> it2 = attachedInstances
                    .iterator(); it2.hasNext();) {
                InstanceDescription instanceDescription = it2.next();
                if (topologyAnnouncement.getOwnerId().equals(
                        instanceDescription.getSlingId())) {
                    logger.info("registerAnnouncement: already have this instance attached: "
                            + instanceDescription.getSlingId());
                    return false;
                }
            }
        }

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);

            final Resource announcementsResource = ResourceHelper
                    .getOrCreateResource(
                            resourceResolver,
                            config.getClusterInstancesPath()
                                    + "/"
                                    + settingsService.getSlingId()
                                    + "/announcements");

            topologyAnnouncement.persistTo(announcementsResource);
            resourceResolver.commit();
            ownAnnouncementsCache.put(topologyAnnouncement.getOwnerId(), 
                    new CachedAnnouncement(topologyAnnouncement));
        } catch (LoginException e) {
            logger.error(
                    "registerAnnouncement: could not log in administratively: "
                            + e, e);
            throw new RuntimeException("Could not log in to repository (" + e
                    + ")", e);
        } catch (PersistenceException e) {
            logger.error("registerAnnouncement: got a PersistenceException: "
                    + e, e);
            throw new RuntimeException(
                    "Exception while talking to repository (" + e + ")", e);
        } catch (JSONException e) {
            logger.error("registerAnnouncement: got a JSONException: " + e, e);
            throw new RuntimeException("Exception while converting json (" + e
                    + ")", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
        return true;
    }

    public synchronized void addAllExcept(final Announcement target, final ClusterView clusterView, 
            final AnnouncementFilter filter) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);

            final Resource clusterInstancesResource = ResourceHelper
                    .getOrCreateResource(
                            resourceResolver,
                            config.getClusterInstancesPath());

            final Iterator<Resource> it0 = clusterInstancesResource.getChildren()
                    .iterator();
            Resource announcementsResource;
            while (it0.hasNext()) {
                final Resource aClusterInstanceResource = it0.next();
                final String instanceId = aClusterInstanceResource.getName();
                //TODO: add ClusterView.contains(instanceSlingId) for convenience to next api change
                if (!contains(clusterView, instanceId)) {
                    // then the instance is not in my view, hence dont propagate
                    // its announcements
                    // (corresponds to earlier expiry-handling)
                    continue;
                }
                announcementsResource = aClusterInstanceResource
                        .getChild("announcements");
                if (announcementsResource == null) {
                    continue;
                }
                Iterator<Resource> it = announcementsResource.getChildren()
                        .iterator();
                while (it.hasNext()) {
                    Resource anAnnouncement = it.next();
                	if (logger.isDebugEnabled()) {
	                    logger.debug("addAllExcept: anAnnouncement="
	                            + anAnnouncement);
                	}
                    Announcement topologyAnnouncement;
                    topologyAnnouncement = Announcement.fromJSON(anAnnouncement
                            .adaptTo(ValueMap.class).get(
                                    "topologyAnnouncement", String.class));
                    if (filter != null && !filter.accept(aClusterInstanceResource.getName(), topologyAnnouncement)) {
                        continue;
                    }
                    target.addIncomingTopologyAnnouncement(topologyAnnouncement);
                }
            }
            resourceResolver.commit();
        } catch (LoginException e) {
            logger.error(
                    "handleEvent: could not log in administratively: " + e, e);
            throw new RuntimeException("Could not log in to repository (" + e
                    + ")", e);
        } catch (PersistenceException e) {
            logger.error("handleEvent: got a PersistenceException: " + e, e);
            throw new RuntimeException(
                    "Exception while talking to repository (" + e + ")", e);
        } catch (JSONException e) {
            logger.error("handleEvent: got a JSONException: " + e, e);
            throw new RuntimeException("Exception while converting json (" + e
                    + ")", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    public synchronized void checkExpiredAnnouncements() {
        final long now = System.currentTimeMillis();
        for (Iterator<Entry<String, CachedAnnouncement>> it = 
                ownAnnouncementsCache.entrySet().iterator(); it.hasNext();) {
            final Entry<String, CachedAnnouncement> entry = it.next();
            final long lastHeartbeat = entry.getValue().getLastHeartbeat();
            if (now-lastHeartbeat>config.getHeartbeatTimeoutMillis()) {
                // then we have an expiry
                it.remove();
                
                final String instanceId = entry.getKey();
                logger.info("checkExpiredAnnouncements: topology connector of "+instanceId+" has expired.");
                deleteAnnouncementsOf(instanceId);
            }
        }
    }

    private final void deleteAnnouncementsOf(final String instanceId) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);
            ResourceHelper.deleteResource(resourceResolver, 
                    config.getClusterInstancesPath()
                                + "/"
                                + settingsService.getSlingId()
                                + "/announcements/"
                                + instanceId);
            resourceResolver.commit();
            resourceResolver.close();
            resourceResolver = null;
        } catch (LoginException e) {
            logger.error(
                    "deleteAnnouncementsOf: could not log in administratively when deleting "
                    + "announcements of instanceId="+instanceId+": " + e, e);
        } catch (PersistenceException e) {
            logger.error(
                    "deleteAnnouncementsOf: got PersistenceException when deleting "
                    + "announcements of instanceId="+instanceId+": " + e, e);
        } finally {
            if (resourceResolver!=null) {
                resourceResolver.revert();
                resourceResolver.close();
                resourceResolver = null;
            }
        }
    }

    public synchronized Collection<InstanceDescription> listInstances(final ClusterView localClusterView) {
        final Collection<InstanceDescription> instances = new LinkedList<InstanceDescription>();

        final Collection<Announcement> announcements = listAnnouncementsInSameCluster(localClusterView);
        if (announcements == null) {
            return instances;
        }

        for (Iterator<Announcement> it = announcements.iterator(); it.hasNext();) {
            final Announcement announcement = it.next();
            instances.addAll(announcement.listInstances());
        }
        return instances;
    }

}
