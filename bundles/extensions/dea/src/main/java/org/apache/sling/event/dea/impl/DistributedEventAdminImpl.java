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
package org.apache.sling.event.dea.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;

/**
 * This service wraps the configuration of the distributed event admin
 * and starts the different parts.
 */
@Component(name="org.apache.sling.event.impl.DistributingEventHandler")
public class DistributedEventAdminImpl {

    public static final String RESOURCE_TYPE_FOLDER = "sling:Folder";

    public static final String RESOURCE_TYPE_EVENT = "sling/distributed/event";

    @Reference
    private SlingSettingsService settings;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private EventAdmin eventAdmin;

    /** Default repository path. */
    public static final String DEFAULT_REPOSITORY_PATH = "/var/eventing/distribution";

    /** The path where all jobs are stored. */
    @Property(value=DEFAULT_REPOSITORY_PATH)
    private static final String CONFIG_PROPERTY_REPOSITORY_PATH = "repository.path";

    /** Default clean up time is 15 minutes. */
    private static final int DEFAULT_CLEANUP_PERIOD = 15;

    @Property(intValue=DEFAULT_CLEANUP_PERIOD)
    private static final String CONFIG_PROPERTY_CLEANUP_PERIOD = "cleanup.period";

    /** The local receiver of distributed events .*/
    private DistributedEventReceiver receiver;

    /** The local sender for distributed events. */
    private DistributedEventSender sender;

    @Activate
    protected void activate(final BundleContext bundleContext, final Map<String, Object> props) {
        final int cleanupPeriod = PropertiesUtil.toInteger(props.get(CONFIG_PROPERTY_CLEANUP_PERIOD), DEFAULT_CLEANUP_PERIOD);
        final String rootPath = PropertiesUtil.toString(props.get(
                CONFIG_PROPERTY_REPOSITORY_PATH), DEFAULT_REPOSITORY_PATH);
        final String ownRootPath = rootPath.concat("/").concat(settings.getSlingId());

        this.receiver = new DistributedEventReceiver(bundleContext,
                rootPath,
                ownRootPath,
                cleanupPeriod,
                this.resourceResolverFactory, this.settings);
        this.sender = new DistributedEventSender(bundleContext,
                              rootPath,
                              ownRootPath,
                              this.resourceResolverFactory, this.eventAdmin);
    }

    @Deactivate
    protected void deactivate() {
        if ( this.receiver != null ) {
            this.receiver.stop();
            this.receiver = null;
        }
        if ( this.sender != null ) {
            this.sender.stop();
            this.sender = null;
        }
    }
}
