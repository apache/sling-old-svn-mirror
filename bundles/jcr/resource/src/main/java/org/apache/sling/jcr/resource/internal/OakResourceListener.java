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

import static java.util.Collections.singletonList;

import java.io.Closeable;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.oak.plugins.observation.NodeObserver;
import org.apache.jackrabbit.oak.spi.commit.BackgroundObserver;
import org.apache.jackrabbit.oak.spi.commit.BackgroundObserverMBean;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.JcrResourceChange.Builder;
import org.apache.sling.jcr.resource.internal.helper.jcr.PathMapper;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ProviderContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@code OakResourceListener} implementation translates and relays
 * all events to an {@link ObservationReporter}
 */
public class OakResourceListener extends NodeObserver implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The repository is mounted under this path. */
    private final String mountPrefix;

    private final ServiceRegistration serviceRegistration;

    private final ServiceRegistration mbeanRegistration;

    private final PathMapper pathMapper;

    private final ProviderContext ctx;

    private final Session session;

    @SuppressWarnings("deprecation")
    public OakResourceListener(
            final String mountPrefix,
            final ProviderContext ctx,
            final BundleContext bundleContext,
            final Executor executor,
            final PathMapper pathMapper,
            final int observationQueueLength,
            final SlingRepository repository)
    throws RepositoryException {
        super(JcrResourceListener.getAbsPath(pathMapper, ctx), "jcr:primaryType", "sling:resourceType", "sling:resourceSuperType");
        this.ctx = ctx;
        this.pathMapper = pathMapper;
        this.mountPrefix = mountPrefix;
        this.session = repository.loginAdministrative(repository.getDefaultWorkspace());

        final Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling JCR Observation Listener for Oak");

        final BackgroundObserver observer = new BackgroundObserver(this, executor, observationQueueLength) {
            @Override
            protected void added(int queueSize) {
                if (queueSize == observationQueueLength) {
                    logger.warn("Revision queue for observer {} is full (max = {}). Further revisions will be compacted.",
                            getClass().getName(), observationQueueLength);
                }
            }
        };
        serviceRegistration = bundleContext.registerService(Observer.class.getName(), observer, props);

        final Dictionary<String, Object> mbeanProps = new Hashtable<String, Object>(props);
        String objectName = String.format("org.apache.sling:type=%s,name=SlingResourceListener",
                BackgroundObserverMBean.TYPE);
        mbeanProps.put("jmx.objectname", objectName);

        mbeanRegistration = bundleContext.registerService(BackgroundObserverMBean.class.getName(), observer.getMBean(), mbeanProps);
    }

    /**
     * Dispose this listener.
     */
    @Override
    public void close() throws IOException {
        mbeanRegistration.unregister();
        serviceRegistration.unregister();
        session.logout();
    }

    @Override
    protected void added(final String path,
            final Set<String> added,
            final Set<String> deleted,
            final Set<String> changed,
            final Map<String, String> properties,
            final CommitInfo commitInfo) {
        final Builder builder = toEventProperties(JcrResourceListener.stripNtFilePath(path, session), added, deleted, changed, commitInfo);
        if (ctx.getExcludedPaths().matches(builder.getPath()) != null) {
            return;
        }
        builder.setChangeType(ChangeType.ADDED);
        if ( logger.isDebugEnabled() ) {
            logger.debug("added(path={}, added={}, deleted={}, changed={})", new Object[] {path, added, deleted, changed});
        }
        ctx.getObservationReporter().reportChanges(singletonList(builder.build()), false);
    }

    @Override
    protected void deleted(final String path,
            final Set<String> added,
            final Set<String> deleted,
            final Set<String> changed,
            final Map<String, String> properties,
            final CommitInfo commitInfo) {
        final Builder builder = toEventProperties(path, added, deleted, changed, commitInfo);
        if (ctx.getExcludedPaths().matches(builder.getPath()) != null) {
            return;
        }
        builder.setChangeType(ChangeType.REMOVED);
        if ( logger.isDebugEnabled() ) {
            logger.debug("deleted(path={}, added={}, deleted={}, changed={})", new Object[] {path, added, deleted, changed});
        }
        ctx.getObservationReporter().reportChanges(singletonList(builder.build()), false);
    }

    @Override
    protected void changed(final String path,
            final Set<String> added,
            final Set<String> deleted,
            final Set<String> changed,
            final Map<String, String> properties,
            final CommitInfo commitInfo) {
        final Builder builder = toEventProperties(JcrResourceListener.stripNtFilePath(path, session), added, deleted, changed, commitInfo);
        if (ctx.getExcludedPaths().matches(builder.getPath()) != null) {
            return;
        }
        builder.setChangeType(ChangeType.CHANGED);
        if ( logger.isDebugEnabled() ) {
            logger.debug("changed(path={}, added={}, deleted={}, changed={})", new Object[] {path, added, deleted, changed});
        }
        ctx.getObservationReporter().reportChanges(singletonList(builder.build()), false);
    }

    private Builder toEventProperties(final String path, final Set<String> added, final Set<String> deleted, final Set<String> changed, final CommitInfo commitInfo) {
        Builder builder = new Builder();
        String pathWithPrefix = JcrResourceListener.addMountPrefix(mountPrefix, path);
        builder.setPath(pathMapper.mapJCRPathToResourcePath(pathWithPrefix));
        if ( added != null && added.size() > 0 ) {
            for (String propName : added) {
                builder.addAddedAttributeName(propName);
            }
        }
        if ( changed != null && changed.size() > 0 ) {
            for (String propName : changed) {
                builder.addChangedAttributeName(propName);
            }
        }
        if ( deleted != null && deleted.size() > 0 ) {
            for (String propName : deleted) {
                builder.addRemovedAttributeName(propName);
            }
        }
        builder.setUserId(commitInfo.getUserId());
        builder.setExternal(false);
        return builder;
    }
}
