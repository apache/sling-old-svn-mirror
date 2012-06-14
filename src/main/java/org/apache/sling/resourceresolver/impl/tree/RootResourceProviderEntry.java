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
package org.apache.sling.jcr.resource.internal.helper;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the root resource provider entry which keeps track
 * of the resource providers.
 */
public class RootResourceProviderEntry extends ResourceProviderEntry {

    /** default logger */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public RootResourceProviderEntry() {
        super("/", null);
    }

    public void bindResourceProvider(final ResourceProvider provider,
                                     final Map<String, Object> props,
                                     final ServiceTracker eventAdminTracker) {

        final String serviceName = getServiceName(provider, props);

        logger.debug("bindResourceProvider: Binding {}", serviceName);

        String[] roots = OsgiUtil.toStringArray(props.get(ResourceProvider.ROOTS));
        if (roots != null && roots.length > 0) {
            final EventAdmin localEA = (EventAdmin) ( eventAdminTracker != null ? eventAdminTracker.getService() : null);

            for (String root : roots) {
                // cut off trailing slash
                if (root.endsWith("/") && root.length() > 1) {
                    root = root.substring(0, root.length() - 1);
                }

                // synchronized insertion of new resource providers into
                // the tree to not inadvertently loose an entry
                synchronized (this) {

                    this.addResourceProvider(root,
                        provider, OsgiUtil.getComparableForServiceRanking(props));
                }
                logger.debug("bindResourceProvider: {}={} ({})",
                    new Object[] { root, provider, serviceName });
                if ( localEA != null ) {
                    final Dictionary<String, Object> eventProps = new Hashtable<String, Object>();
                    eventProps.put(SlingConstants.PROPERTY_PATH, root);
                    localEA.postEvent(new Event(SlingConstants.TOPIC_RESOURCE_PROVIDER_ADDED,
                            eventProps));
                }
            }
        }

        logger.debug("bindResourceProvider: Bound {}", serviceName);
    }

    public void unbindResourceProvider(final ResourceProvider provider,
                                       final Map<String, Object> props,
                                       final ServiceTracker eventAdminTracker) {

        final String serviceName = getServiceName(provider, props);

        logger.debug("unbindResourceProvider: Unbinding {}", serviceName);

        String[] roots = OsgiUtil.toStringArray(props.get(ResourceProvider.ROOTS));
        if (roots != null && roots.length > 0) {

            final EventAdmin localEA = (EventAdmin) ( eventAdminTracker != null ? eventAdminTracker.getService() : null);

            for (String root : roots) {
                // cut off trailing slash
                if (root.endsWith("/") && root.length() > 1) {
                    root = root.substring(0, root.length() - 1);
                }

                // synchronized insertion of new resource providers into
                // the tree to not inadvertently loose an entry
                synchronized (this) {
                    // TODO: Do not remove this path, if another resource
                    // owns it. This may be the case if adding the provider
                    // yielded an ResourceProviderEntryException
                    this.removeResourceProvider(root, provider, OsgiUtil.getComparableForServiceRanking(props));
                }
                logger.debug("unbindResourceProvider: root={} ({})", root,
                    serviceName);
                if ( localEA != null ) {
                    final Dictionary<String, Object> eventProps = new Hashtable<String, Object>();
                    eventProps.put(SlingConstants.PROPERTY_PATH, root);
                    localEA.postEvent(new Event(SlingConstants.TOPIC_RESOURCE_PROVIDER_REMOVED,
                            eventProps));
                }
            }
        }

        logger.debug("unbindResourceProvider: Unbound {}", serviceName);
    }

    private String getServiceName(final ResourceProvider provider, final Map<String, Object> props) {
        if (logger.isDebugEnabled()) {
            StringBuilder snBuilder = new StringBuilder(64);
            snBuilder.append('{');
            snBuilder.append(provider.toString());
            snBuilder.append('/');
            snBuilder.append(props.get(Constants.SERVICE_ID));
            snBuilder.append('}');
            return snBuilder.toString();
        }

        return null;
    }
}