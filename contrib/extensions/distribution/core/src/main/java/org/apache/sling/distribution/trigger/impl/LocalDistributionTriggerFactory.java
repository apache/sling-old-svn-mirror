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
package org.apache.sling.distribution.trigger.impl;

import javax.annotation.Nonnull;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.component.impl.DistributionComponentUtils;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.DistributionTriggerException;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;

@Component(metatype = true,
        label = "Sling Distribution - Generic Local Triggers Factory",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Service(DistributionTrigger.class)
public class LocalDistributionTriggerFactory implements DistributionTrigger {


    /**
     * remote event trigger type
     */
    public static final String TRIGGER_REMOTE_EVENT = "remoteEvent";

    /**
     * remote event endpoint property
     */
    public static final String TRIGGER_REMOTE_EVENT_PROPERTY_ENDPOINT = "endpoint";

    /**
     * resource event trigger type
     */
    public static final String TRIGGER_RESOURCE_EVENT = "resourceEvent";

    /**
     * resource event path property
     */
    public static final String TRIGGER_RESOURCE_EVENT_PROPERTY_PATH = "path";

    /**
     * scheduled trigger type
     */
    public static final String TRIGGER_SCHEDULED_EVENT = "scheduledEvent";

    /**
     * scheduled trigger action property
     */
    public static final String TRIGGER_SCHEDULED_EVENT_PROPERTY_ACTION = "action";

    /**
     * scheduled trigger path property
     */
    public static final String TRIGGER_SCHEDULED_EVENT_PROPERTY_PATH = "path";

    /**
     * scheduled trigger seconds property
     */
    public static final String TRIGGER_SCHEDULED_EVENT_PROPERTY_SECONDS = "seconds";

    /**
     * chain distribution trigger type
     */
    public static final String TRIGGER_DISTRIBUTION_EVENT = "distributionEvent";

    /**
     * chain distribution path property
     */
    public static final String TRIGGER_DISTRIBUTION_EVENT_PROPERTY_PATH = "path";

    /**
     * jcr event trigger type
     */
    public static final String TRIGGER_JCR_EVENT = "jcrEvent";

    /**
     * jcr event trigger path property
     */
    public static final String TRIGGER_JCR_EVENT_PROPERTY_PATH = "path";

    /**
     * jcr event trigger service user property
     */
    public static final String TRIGGER_JCR_EVENT_PROPERTY_SERVICE_NAME = "servicename";

    /**
     * jcr persisting event trigger type
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT = "persistedJcrEvent";

    /**
     * jcr persisting event trigger path property
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_PATH = "path";

    /**
     * jcr persisting event trigger service user property
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_SERVICE_NAME = "servicename";

    /**
     * jcr persisting event trigger nuggets path property
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_NUGGETS_PATH = "nuggetsPath";



    DistributionTrigger trigger;

    @Reference
    private SlingRepository repository;

    @Reference
    private Scheduler scheduler;


    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> config) {
        String factory = PropertiesUtil.toString(config.get(DistributionComponentUtils.PN_TYPE), null);

        if (TRIGGER_RESOURCE_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(config.get(TRIGGER_RESOURCE_EVENT_PROPERTY_PATH), null);

            trigger = new ResourceEventDistributionTrigger(path, bundleContext);
        } else if (TRIGGER_SCHEDULED_EVENT.equals(factory)) {
            String action = PropertiesUtil.toString(config.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_ACTION), DistributionRequestType.PULL.name());
            String path = PropertiesUtil.toString(config.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_PATH), "/");
            int interval = PropertiesUtil.toInteger(config.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_SECONDS), 30);

            trigger =  new ScheduledDistributionTrigger(action, path, interval, scheduler);
        } else if (TRIGGER_DISTRIBUTION_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(config.get(TRIGGER_DISTRIBUTION_EVENT_PROPERTY_PATH), null);

            trigger =  new ChainDistributeDistributionTrigger(path, bundleContext);
        } else if (TRIGGER_JCR_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(config.get(TRIGGER_JCR_EVENT_PROPERTY_PATH), null);
            String serviceName = PropertiesUtil.toString(config.get(TRIGGER_JCR_EVENT_PROPERTY_SERVICE_NAME), null);

            trigger =  new JcrEventDistributionTrigger(repository, path, serviceName);
        } else if (TRIGGER_PERSISTED_JCR_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(config.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_PATH), null);
            String serviceName = PropertiesUtil.toString(config.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_SERVICE_NAME), null);
            String nuggetsPath = PropertiesUtil.toString(config.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_NUGGETS_PATH), null);

            trigger =  new PersistingJcrEventDistributionTrigger(repository, path, serviceName, nuggetsPath);
        }
    }

    @Deactivate
    public void deactivate() {
        if (trigger instanceof ScheduledDistributionTrigger) {
            ((ScheduledDistributionTrigger) trigger).unregisterAll();
        }
    }

    public void register(@Nonnull DistributionRequestHandler requestHandler) throws DistributionTriggerException {
        trigger.register(requestHandler);
    }

    public void unregister(@Nonnull DistributionRequestHandler requestHandler) throws DistributionTriggerException {
        trigger.unregister(requestHandler);
    }
}
