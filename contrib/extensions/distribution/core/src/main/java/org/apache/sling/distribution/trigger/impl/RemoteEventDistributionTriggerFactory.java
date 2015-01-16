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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.component.impl.DistributionComponentUtils;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.DistributionTriggerException;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;

import javax.annotation.Nonnull;
import java.util.Map;

@Component(metatype = true,
        label = "Sling Distribution - Remote Event Triggers Factory",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Service(DistributionTrigger.class)
public class RemoteEventDistributionTriggerFactory implements DistributionTrigger {


    /**
     * remote event trigger type
     */
    public static final String TRIGGER_REMOTE_EVENT = "remoteEvent";

    /**
     * remote event endpoint property
     */
    @Property
    public static final String ENDPOINT = "endpoint";


    @Property(name = "transportSecretProvider.target")
    @Reference(name = "transportSecretProvider")
    DistributionTransportSecretProvider transportSecretProvider;


    @Reference
    private Scheduler scheduler;

    RemoteEventDistributionTrigger trigger;



    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> config) {
        String endpoint = PropertiesUtil.toString(config.get(ENDPOINT), null);
        trigger = new RemoteEventDistributionTrigger(endpoint, transportSecretProvider, scheduler);
    }

    @Deactivate
    public void deactivate() {
        trigger.disable();

    }

    public void register(@Nonnull DistributionRequestHandler requestHandler) throws DistributionTriggerException {
        trigger.register(requestHandler);
    }

    public void unregister(@Nonnull DistributionRequestHandler requestHandler) throws DistributionTriggerException {
        trigger.unregister(requestHandler);
    }
}
