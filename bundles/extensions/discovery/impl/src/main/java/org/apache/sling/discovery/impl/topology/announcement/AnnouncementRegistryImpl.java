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
import java.util.Iterator;
import java.util.LinkedList;

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

    public void unregisterAnnouncement(final String ownerId) {
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

    public Collection<Announcement> listAnnouncements(final ListScope scope) {
        ResourceResolver resourceResolver = null;
        final Collection<Announcement> incomingAnnouncements = new LinkedList<Announcement>();
        try {
            resourceResolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);

            if (scope != ListScope.AllInSameCluster) {
                final Resource announcementsResource = ResourceHelper
                        .getOrCreateResource(
                                resourceResolver,
                                config.getClusterInstancesPath()
                                        + "/"
                                        + settingsService.getSlingId()
                                        + "/announcements");

                final Iterator<Resource> it = announcementsResource.getChildren()
                        .iterator();
                Announcement topologyAnnouncement;
                while (it.hasNext()) {
                    Resource anAnnouncement = it.next();
                	if (logger.isDebugEnabled()) {
	                    logger.debug("listIncomingAnnouncements: anAnnouncement="
	                            + anAnnouncement);
                	}
                    topologyAnnouncement = Announcement.fromJSON(anAnnouncement
                            .adaptTo(ValueMap.class).get(
                                    "topologyAnnouncement", String.class));

                    if (scope != ListScope.OnlyInherited
                            || !topologyAnnouncement.isInherited()) {
                        incomingAnnouncements.add(topologyAnnouncement);
                    }
                }
            } else {
                Resource clusterInstancesResource = ResourceHelper
                        .getOrCreateResource(
                                resourceResolver,
                                config.getClusterInstancesPath());

                Iterator<Resource> it0 = clusterInstancesResource.getChildren()
                        .iterator();
                Resource announcementsResource;
                while (it0.hasNext()) {
                    Resource aClusterInstanceResource = it0.next();
                    announcementsResource = aClusterInstanceResource
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
                        if (!topologyAnnouncement.hasExpired(config)) {
                            incomingAnnouncements.add(topologyAnnouncement);
                        } // else another node's announcement has expired - but
                          // it's the job of the other node
                          // to cleanup - this node just filters
                    }
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
    	if (logger.isDebugEnabled()) {
    		logger.debug("listAnnouncements: result: "+incomingAnnouncements.size());
    	}
        return incomingAnnouncements;
    }

    public boolean registerAnnouncement(final Announcement topologyAnnouncement) {
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

        final Collection<Announcement> announcements = listAnnouncements(ListScope.AllLocally);
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

    public void addAllExcept(final Announcement target, final AnnouncementFilter filter) {
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
                    if (topologyAnnouncement.hasExpired(config)) {
                        // dont propagate announcements that have expired
                        continue;
                    }
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

    public void checkExpiredAnnouncements() {
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

            final Iterator<Resource> it = announcementsResource.getChildren()
                    .iterator();
            Announcement topologyAnnouncement;
            while (it.hasNext()) {
                Resource anAnnouncement = it.next();
            	if (logger.isDebugEnabled()) {
	                logger.debug("checkExpiredAnnouncements: anAnnouncement="
	                        + anAnnouncement);
            	}
                topologyAnnouncement = Announcement.fromJSON(anAnnouncement
                        .adaptTo(ValueMap.class).get("topologyAnnouncement",
                                String.class));
                if (topologyAnnouncement.hasExpired(config)) {
                    logger.info("checkExpiredAnnouncements: topology announcement has expired: "
                            + anAnnouncement);
                    resourceResolver.delete(anAnnouncement);
                } else {
                	if (logger.isDebugEnabled()) {
	                    logger.debug("checkExpiredAnnouncements: topology announcement still valid: "
	                            + anAnnouncement);
                	}

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

    public Collection<InstanceDescription> listInstances() {
        final Collection<InstanceDescription> instances = new LinkedList<InstanceDescription>();

        final Collection<Announcement> announcements = listAnnouncements(ListScope.AllInSameCluster);
        if (announcements == null) {
            return instances;
        }

        for (Iterator<Announcement> it = announcements.iterator(); it.hasNext();) {
            Announcement announcement = it.next();
            instances.addAll(announcement.listInstances());
        }
        return instances;
    }

}
