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
package org.apache.sling.discovery.base.connectors.announcement;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.JsonException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.base.connectors.BaseConfig;
import org.apache.sling.discovery.commons.providers.util.ResourceHelper;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the AnnouncementRegistry which
 * handles JSON-backed announcements and does so by storing
 * them in a local like /var/discovery/impl/clusterNodes/$slingId/announcement.
 */
@Component
@Service(value = AnnouncementRegistry.class)
public class AnnouncementRegistryImpl implements AnnouncementRegistry {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SlingSettingsService settingsService;

    private String slingId;

    @Reference
    private BaseConfig config;

    public static AnnouncementRegistryImpl testConstructorAndActivate(ResourceResolverFactory resourceResolverFactory,
            SlingSettingsService slingSettingsService, BaseConfig config) {
        AnnouncementRegistryImpl registry = testConstructor(resourceResolverFactory, slingSettingsService, config);
        registry.activate();
        return registry;
    }

    public static AnnouncementRegistryImpl testConstructor(ResourceResolverFactory resourceResolverFactory,
            SlingSettingsService slingSettingsService, BaseConfig config) {
        AnnouncementRegistryImpl registry = new AnnouncementRegistryImpl();
        registry.resourceResolverFactory = resourceResolverFactory;
        registry.settingsService = slingSettingsService;
        registry.config = config;
        return registry;
    }

    @Activate
    protected void activate() {
        slingId = settingsService.getSlingId();
    }

    private final Map<String,CachedAnnouncement> ownAnnouncementsCache =
            new HashMap<String,CachedAnnouncement>();

    @Override
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
                    .getServiceResourceResolver(null);

            final String path = config.getClusterInstancesPath()
                    + "/"
                    + slingId
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

    @Override
    public synchronized Collection<Announcement> listLocalAnnouncements() {
        return fillWithCachedAnnouncements(new LinkedList<Announcement>());
    }

    @Override
    public synchronized Collection<CachedAnnouncement> listLocalIncomingAnnouncements() {
        Collection<CachedAnnouncement> result = new LinkedList<CachedAnnouncement>(ownAnnouncementsCache.values());
        for (Iterator<CachedAnnouncement> it = result.iterator(); it.hasNext();) {
            CachedAnnouncement cachedAnnouncement = it.next();
            if (cachedAnnouncement.getAnnouncement().isInherited()) {
                it.remove();
                continue;
            }
            if (cachedAnnouncement.hasExpired()) {
                it.remove();
                continue;
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

    @Override
    public synchronized Collection<Announcement> listAnnouncementsInSameCluster(final ClusterView localClusterView) {
        logger.debug("listAnnouncementsInSameCluster: start. localClusterView: {}", localClusterView);
        if (localClusterView==null) {
            throw new IllegalArgumentException("clusterView must not be null");
        }
        ResourceResolver resourceResolver = null;
        final Collection<Announcement> incomingAnnouncements = new LinkedList<Announcement>();
        final InstanceDescription localInstance = getLocalInstanceDescription(localClusterView);
        try {
            resourceResolver = resourceResolverFactory
                    .getServiceResourceResolver(null);

            Resource clusterInstancesResource = ResourceHelper
                    .getOrCreateResource(
                            resourceResolver,
                            config.getClusterInstancesPath());

            Iterator<Resource> it0 = clusterInstancesResource.getChildren()
                    .iterator();
            while (it0.hasNext()) {
                Resource aClusterInstanceResource = it0.next();
                final String instanceId = aClusterInstanceResource.getName();
                logger.debug("listAnnouncementsInSameCluster: handling clusterInstance: {}", instanceId);
                if (localInstance!=null && localInstance.getSlingId().equals(instanceId)) {
                    // this is the local instance then - which we serve from the cache only
                    logger.debug("listAnnouncementsInSameCluster: matched localInstance, filling with cache: {}", instanceId);
                    fillWithCachedAnnouncements(incomingAnnouncements);
                    continue;
                }

                //TODO: add ClusterView.contains(instanceSlingId) for convenience to next api change
                if (!contains(localClusterView, instanceId)) {
                    logger.debug("listAnnouncementsInSameCluster: instance is not in my view, ignoring: {}", instanceId);
                    // then the instance is not in my view, hence ignore its announcements
                    // (corresponds to earlier expiry-handling)
                    continue;
                }
                final Resource announcementsResource = aClusterInstanceResource
                        .getChild("announcements");
                if (announcementsResource == null) {
                    logger.debug("listAnnouncementsInSameCluster: instance has no announcements: {}", instanceId);
                    continue;
                }
                logger.debug("listAnnouncementsInSameCluster: instance has announcements: {}", instanceId);
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
                    logger.debug("listAnnouncementsInSameCluster: found announcement: {}", topologyAnnouncement);
                    incomingAnnouncements.add(topologyAnnouncement);
                    // SLING-3389: no longer check for expired announcements -
                    // instead make use of the fact that this instance
                    // has a clusterView and that every live instance
                    // is responsible of cleaning up expired announcements
                    // with the repository
                }
            }
            // since SLING-3389 this method does only read operations, hence
            // no commit necessary anymore - close happens in below finally block
        } catch (LoginException e) {
            logger.error(
                    "listAnnouncementsInSameCluster: could not log in administratively: " + e, e);
            throw new RuntimeException("Could not log in to repository (" + e
                    + ")", e);
        } catch (PersistenceException e) {
            logger.error("listAnnouncementsInSameCluster: got a PersistenceException: " + e, e);
            throw new RuntimeException(
                    "Exception while talking to repository (" + e + ")", e);
        } catch (JsonException e) {
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
            if (entry.getValue().hasExpired()) {
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

    @Override
    public synchronized boolean hasActiveAnnouncement(final String ownerId) {
        if (ownerId==null || ownerId.length()==0) {
            throw new IllegalArgumentException("ownerId must not be null or empty: "+ownerId);
        }
        final CachedAnnouncement cachedAnnouncement = ownAnnouncementsCache.get(ownerId);
        if (cachedAnnouncement==null) {
            return false;
        }

        return !cachedAnnouncement.hasExpired();
    }

    @Override
    public synchronized long registerAnnouncement(final Announcement topologyAnnouncement) {
        if (topologyAnnouncement==null) {
            throw new IllegalArgumentException("topologyAnnouncement must not be null");
        }
        if (!topologyAnnouncement.isValid()) {
            logger.warn("topologyAnnouncement is not valid");
            return -1;
        }
        if (resourceResolverFactory == null) {
            logger.error("registerAnnouncement: resourceResolverFactory is null");
            return -1;
        }

        final CachedAnnouncement cachedAnnouncement =
                ownAnnouncementsCache.get(topologyAnnouncement.getOwnerId());
        if (cachedAnnouncement!=null) {
            if (logger.isDebugEnabled()) {
                logger.debug("registerAnnouncement: got existing cached announcement for ownerId="+topologyAnnouncement.getOwnerId());
            }
            try{
                if (topologyAnnouncement.correspondsTo(cachedAnnouncement.getAnnouncement())) {
                    // then nothing has changed with this announcement, so just update
                    // the heartbeat and fine is.
                    // this should actually be the normal case for a stable connector
                    logger.debug("registerAnnouncement: nothing has changed, only updating heartbeat in-memory.");
                    return cachedAnnouncement.registerPing(topologyAnnouncement, config);
                }
                logger.debug("registerAnnouncement: incoming announcement differs from existing one!");

            } catch(JsonException e) {
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
                    return -1;
                }
            }
        }

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory
                    .getServiceResourceResolver(null);

            final Resource announcementsResource = ResourceHelper
                    .getOrCreateResource(
                            resourceResolver,
                            config.getClusterInstancesPath()
                                    + "/"
                                    + slingId
                                    + "/announcements");

            topologyAnnouncement.persistTo(announcementsResource);
            resourceResolver.commit();
            ownAnnouncementsCache.put(topologyAnnouncement.getOwnerId(),
                    new CachedAnnouncement(topologyAnnouncement, config));
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
        } catch (JsonException e) {
            logger.error("registerAnnouncement: got a JSONException: " + e, e);
            throw new RuntimeException("Exception while converting json (" + e
                    + ")", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
        return 0;
    }

    @Override
    public synchronized void addAllExcept(final Announcement target, final ClusterView clusterView,
            final AnnouncementFilter filter) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory
                    .getServiceResourceResolver(null);

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
            // even before SLING-3389 this method only did read operations,
            // hence no commit was ever necessary. The close happens in the finally block
        } catch (LoginException e) {
            logger.error(
                    "handleEvent: could not log in administratively: " + e, e);
            throw new RuntimeException("Could not log in to repository (" + e
                    + ")", e);
        } catch (PersistenceException e) {
            logger.error("handleEvent: got a PersistenceException: " + e, e);
            throw new RuntimeException(
                    "Exception while talking to repository (" + e + ")", e);
        } catch (JsonException e) {
            logger.error("handleEvent: got a JSONException: " + e, e);
            throw new RuntimeException("Exception while converting json (" + e
                    + ")", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    @Override
    public synchronized void checkExpiredAnnouncements() {
        for (Iterator<Entry<String, CachedAnnouncement>> it =
                ownAnnouncementsCache.entrySet().iterator(); it.hasNext();) {
            final Entry<String, CachedAnnouncement> entry = it.next();
            if (entry.getValue().hasExpired()) {
                // then we have an expiry
                it.remove();

                final String instanceId = entry.getKey();
                logger.info("checkExpiredAnnouncements: topology connector of "+instanceId+
                        " (to me="+slingId+
                        ", inherited="+entry.getValue().getAnnouncement().isInherited()+") has expired.");
                deleteAnnouncementsOf(instanceId);
            }
        }
        //SLING-4139 : also make sure there are no stale announcements
        //             in the repository (from a crash or any other action).
        //             The ownAnnouncementsCache is the authorative set
        //             of announcements that are registered to this
        //             instance's registry - and the repository must not
        //             contain any additional announcements
        ResourceResolver resourceResolver = null;
        boolean requiresCommit = false;
        try {
            resourceResolver = resourceResolverFactory
                    .getServiceResourceResolver(null);
            final Resource announcementsResource = ResourceHelper
                    .getOrCreateResource(
                            resourceResolver,
                            config.getClusterInstancesPath()
                                    + "/"
                                    + slingId
                                    + "/announcements");
            final Iterator<Resource> it = announcementsResource.getChildren().iterator();
            while(it.hasNext()) {
            	final Resource res = it.next();
            	final String ownerId = res.getName();
            	// ownerId is the slingId of the owner of the announcement (ie of the peer of the connector).
            	// let's check if the we have that owner's announcement in the cache

            	if (ownAnnouncementsCache.containsKey(ownerId)) {
            		// fine then, we'll leave this announcement untouched
            		continue;
            	}
            	// otherwise this announcement is likely from an earlier incarnation
            	// of this instance - hence stale - hence we must remove it now
            	//  (SLING-4139)
            	ResourceHelper.deleteResource(resourceResolver,
            			res.getPath());
            	requiresCommit = true;
            }
            if (requiresCommit) {
                resourceResolver.commit();
            }
            resourceResolver.close();
            resourceResolver = null;
        } catch (LoginException e) {
            logger.error(
                    "checkExpiredAnnouncements: could not log in administratively when checking "
                    + "for expired announcements of slingId="+slingId+": " + e, e);
        } catch (PersistenceException e) {
            logger.error(
                    "checkExpiredAnnouncements: got PersistenceException when checking "
                    + "for expired announcements of slingId="+slingId+": " + e, e);
        } finally {
            if (resourceResolver!=null) {
                resourceResolver.revert();
                resourceResolver.close();
                resourceResolver = null;
            }
        }
    }

    private final void deleteAnnouncementsOf(final String instanceId) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory
                    .getServiceResourceResolver(null);
            ResourceHelper.deleteResource(resourceResolver,
                    config.getClusterInstancesPath()
                                + "/"
                                + slingId
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

    @Override
    public synchronized Collection<InstanceDescription> listInstances(final ClusterView localClusterView) {
        logger.debug("listInstances: start. localClusterView: {}", localClusterView);
        final Collection<InstanceDescription> instances = new LinkedList<InstanceDescription>();

        final Collection<Announcement> announcements = listAnnouncementsInSameCluster(localClusterView);
        if (announcements == null) {
            logger.debug("listInstances: no announcement found. end. instances: {}", instances);
            return instances;
        }

        for (Iterator<Announcement> it = announcements.iterator(); it.hasNext();) {
            final Announcement announcement = it.next();
            logger.debug("listInstances: adding announcement: {}", announcement);
            instances.addAll(announcement.listInstances());
        }
        logger.debug("listInstances: announcements added. end. instances: {}", instances);
        return instances;
    }

}
