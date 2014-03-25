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
package org.apache.sling.jcr.resource.internal;

import static org.apache.sling.api.SlingConstants.PROPERTY_ADDED_ATTRIBUTES;
import static org.apache.sling.api.SlingConstants.PROPERTY_CHANGED_ATTRIBUTES;
import static org.apache.sling.api.SlingConstants.PROPERTY_PATH;
import static org.apache.sling.api.SlingConstants.PROPERTY_REMOVED_ATTRIBUTES;
import static org.apache.sling.api.SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE;
import static org.apache.sling.api.SlingConstants.PROPERTY_RESOURCE_TYPE;
import static org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_ADDED;
import static org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_CHANGED;
import static org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_REMOVED;

import java.io.Closeable;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.oak.plugins.observation.NodeObserver;
import org.apache.jackrabbit.oak.spi.commit.BackgroundObserver;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@code OakResourceListener} implementation translates and relays
 * all events to the OSGi {@code EventAdmin}.
 */
public class OakResourceListener extends NodeObserver implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The repository is mounted under this path. */
    private final String mountPrefix;

    private final ServiceRegistration serviceRegistration;

    /** Helper object. */
    final ObservationListenerSupport support;

    public OakResourceListener(
            final String mountPrefix,
            final ObservationListenerSupport support,
            final BundleContext bundleContext,
            final Executor executor)
    throws RepositoryException {
        super("/", "jcr:primaryType", "sling:resourceType", "sling:resourceSuperType");
        this.support = support;
        this.mountPrefix = (mountPrefix == null || mountPrefix.length() == 0 || mountPrefix.equals("/") ? null : mountPrefix);

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling JCR Observation Listener for Oak");

        final Observer observer = new BackgroundObserver(this, executor);
        serviceRegistration = bundleContext.registerService(Observer.class.getName(), observer, props);
    }

    /**
     * Dispose this listener.
     */
    public void close() throws IOException {
        serviceRegistration.unregister();
        this.support.dispose();
    }

    @Override
    protected void added(final String path,
            final Set<String> added,
            final Set<String> deleted,
            final Set<String> changed,
            final Map<String, String> properties,
            final CommitInfo commitInfo) {
        final Map<String, Object> changes = toEventProperties(added, deleted, changed);
        addCommitInfo(changes, commitInfo);
        sendOsgiEvent(path, TOPIC_RESOURCE_ADDED, changes, properties);
    }

    @Override
    protected void deleted(final String path,
            final Set<String> added,
            final Set<String> deleted,
            final Set<String> changed,
            final Map<String, String> properties,
            final CommitInfo commitInfo) {
        final Map<String, Object> changes = toEventProperties(added, deleted, changed);
        addCommitInfo(changes, commitInfo);
        sendOsgiEvent(path, TOPIC_RESOURCE_REMOVED, changes, properties);
    }

    @Override
    protected void changed(final String path,
            final Set<String> added,
            final Set<String> deleted,
            final Set<String> changed,
            final Map<String, String> properties,
            final CommitInfo commitInfo) {
        final Map<String, Object> changes = toEventProperties(added, deleted, changed);
        addCommitInfo(changes, commitInfo);
        sendOsgiEvent(path, TOPIC_RESOURCE_CHANGED, changes, properties);
    }

    private static void addCommitInfo(final Map<String, Object> changes, final CommitInfo commitInfo) {
        if ( commitInfo.getUserId() != null ) {
            changes.put(SlingConstants.PROPERTY_USERID, commitInfo.getUserId());
        }
        if (commitInfo == CommitInfo.EMPTY) {
            changes.put("event.application", "unknown");
        }
    }

    private static Map<String, Object> toEventProperties(final Set<String> added, final Set<String> deleted, final Set<String> changed) {
        final Map<String, Object> properties = new HashMap<String, Object>();
        if ( added != null && added.size() > 0 ) {
            properties.put(PROPERTY_ADDED_ATTRIBUTES, added.toArray(new String[added.size()]));
        }
        if ( changed != null && changed.size() > 0 ) {
            properties.put(PROPERTY_CHANGED_ATTRIBUTES, changed.toArray(new String[changed.size()]));
        }
        if ( deleted != null && deleted.size() > 0 ) {
            properties.put(PROPERTY_REMOVED_ATTRIBUTES, deleted.toArray(new String[deleted.size()]));
        }
        return properties;
    }

    private void sendOsgiEvent(final String path,
            final String topic,
            final Map<String, Object> changes,
            final Map<String, String> properties) {
        // set the path (will be changed for nt:file jcr:content sub resource)
        if ( this.mountPrefix == null ) {
            changes.put(PROPERTY_PATH, path);
        } else {
            changes.put(PROPERTY_PATH, this.mountPrefix + path);
        }

        try {
            final EventAdmin localEa = this.support.getEventAdmin();
            if (localEa != null ) {
                boolean sendEvent = true;
                if (!TOPIC_RESOURCE_REMOVED.equals(topic)) {
                    String resourceType = properties.get("sling:resourceType");
                    String resourceSuperType = properties.get("sling:resourceSuperType");
                    String nodeType = properties.get("jcr:primaryType");

                    // check for nt:file nodes
                    if (path.endsWith("/jcr:content")) {
                        final ResourceResolver resolver = this.support.getResourceResolver();
                        if ( resolver == null ) {
                            sendEvent = false;
                        } else {
                            final Resource rsrc = resolver.getResource((String)changes.get(PROPERTY_PATH));
                            if ( rsrc == null ) {
                                sendEvent = false;
                            } else {
                                // check if this is a JCR backed resource, otherwise it is not visible!
                                final Node node = rsrc.adaptTo(Node.class);
                                if (node != null) {
                                    try {
                                        if (node.getParent().isNodeType("nt:file")) {
                                            final Resource parentResource = rsrc.getParent();
                                            if (parentResource != null) {
                                                // update resource type and path to parent node
                                                resourceType = parentResource.getResourceType();
                                                resourceSuperType = parentResource.getResourceSuperType();
                                                changes.put(PROPERTY_PATH, parentResource.getPath());
                                            }
                                        }
                                    } catch (RepositoryException re) {
                                        // ignore this
                                    }

                                } else {
                                    // this is not a jcr backed resource
                                    sendEvent = false;
                                }

                            }
                        }
                        if ( !sendEvent ) {
                            // take a quite silent note of not being able to
                            // resolve the resource
                            logger.debug(
                                "processOsgiEventQueue: Resource at {} not found, which is not expected for an added or modified node",
                                    path);
                        }
                    }

                    // update resource type properties
                    if ( sendEvent ) {
                        if ( resourceType == null ) {
                            changes.put(PROPERTY_RESOURCE_TYPE, nodeType.replace(':', '/'));
                        } else {
                            changes.put(PROPERTY_RESOURCE_TYPE, resourceType);
                        }
                        if ( resourceSuperType != null ) {
                            changes.put(PROPERTY_RESOURCE_SUPER_TYPE, resourceSuperType);
                        }
                    }
                }

                if ( sendEvent ) {
                    localEa.sendEvent(new org.osgi.service.event.Event(topic, new EventProperties(changes)));
                }
            }
        } catch (final Exception e) {
            logger.warn("sendOsgiEvent: Unexpected problem processing event " + topic + " at " + path + " with " + changes, e);
        }
    }
}
