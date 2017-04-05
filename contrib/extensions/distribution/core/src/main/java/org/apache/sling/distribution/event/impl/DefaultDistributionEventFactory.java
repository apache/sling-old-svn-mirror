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
package org.apache.sling.distribution.event.impl;

import javax.annotation.Nonnull;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.event.DistributionEventProperties;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DistributionEventFactory} OSGi service
 */
@Component(immediate = true)
@Service(value = DistributionEventFactory.class)
public class DefaultDistributionEventFactory implements DistributionEventFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private EventAdmin eventAdmin;

    private void generateEvent(@Nonnull String distributionEventTopic, @Nonnull Dictionary<?, ?> properties) {
        eventAdmin.postEvent(new Event(distributionEventTopic, properties));
        log.debug("distribution event {} posted", distributionEventTopic);
    }

    public void generatePackageEvent(@Nonnull String distributionEventTopic, @Nonnull DistributionComponentKind kind,
                                     @Nonnull String name, @Nonnull DistributionPackageInfo info) {
        try {
            Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
            dictionary.put(DistributionEventProperties.DISTRIBUTION_COMPONENT_NAME, name);
            dictionary.put(DistributionEventProperties.DISTRIBUTION_COMPONENT_KIND, kind.name());
            if (info.getRequestType() != null) {
                dictionary.put(DistributionEventProperties.DISTRIBUTION_TYPE, info.getRequestType());
            }
            if (info.getPaths() != null) {
                dictionary.put(DistributionEventProperties.DISTRIBUTION_PATHS, info.getPaths());
            }
            generateEvent(distributionEventTopic, dictionary);

        } catch (Throwable e) {
            log.error("Cannot generate package event", e);
        }

    }

}
