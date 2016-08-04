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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;

@Component(metatype = true,
        label = "Apache Sling Distribution Trigger - Jcr Event Triggers Factory",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Service(DistributionTrigger.class)
@Property(name="webconsole.configurationFactory.nameHint", value="Trigger name: {name} on path {path}")
public class JcrEventDistributionTriggerFactory implements DistributionTrigger {


    @Property(label = "Name", description = "The name of the trigger.")
    public static final String NAME = DistributionComponentConstants.PN_NAME;

    /**
     * jcr event trigger path property
     */
    @Property(label = "Path", description = "The path for which changes are distributed.")
    private static final String PATH = "path";

    /**
     * jcr event trigger path property
     */
    @Property(cardinality = 100, label = "Ignored Paths Patterns", description = "The paths matching one of these patterns will be ignored.")
    private static final String IGNORED_PATHS_PATTERNS = "ignoredPathsPatterns";

    /**
     * jcr event trigger service user property
     */
    @Property(label = "Service Name", description = "The service used to listen for jcr events")
    private static final String SERVICE_NAME = "serviceName";


    private JcrEventDistributionTrigger trigger;

    @Reference
    private SlingRepository repository;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resolverFactory;


    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> config) {
        String path = PropertiesUtil.toString(config.get(PATH), null);
        String serviceName = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(SERVICE_NAME), null));
        String[] ignoredPathsPatterns = PropertiesUtil.toStringArray(config.get(IGNORED_PATHS_PATTERNS), null);
        ignoredPathsPatterns = SettingsUtils.removeEmptyEntries(ignoredPathsPatterns);


        trigger = new JcrEventDistributionTrigger(repository, scheduler, resolverFactory, path, serviceName, ignoredPathsPatterns);
        trigger.enable();
    }

    @Deactivate
    public void deactivate() {
        trigger.disable();
    }

    public void register(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException {
        trigger.register(requestHandler);
    }

    public void unregister(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException {
        trigger.unregister(requestHandler);
    }
}
