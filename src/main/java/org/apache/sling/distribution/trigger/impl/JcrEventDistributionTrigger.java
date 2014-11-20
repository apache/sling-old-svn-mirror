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

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.sling.distribution.communication.DistributionActionType;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JCR observation based {@link org.apache.sling.distribution.trigger.DistributionTrigger}.
 */
public class JcrEventDistributionTrigger extends AbstractJcrEventTrigger implements DistributionTrigger {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public JcrEventDistributionTrigger(SlingRepository repository, String path, String serviceName) {
        super(repository, path, serviceName);
    }

    @Override
    protected DistributionRequest processEvent(Event event) throws RepositoryException {
        log.info("triggering distribution from jcr event {}", event);
        DistributionRequest distributionRequest = null;
        Object pathProperty = event.getPath();
        if (pathProperty != null) {
            String replicatingPath = String.valueOf(pathProperty);
            int type = event.getType();
            if (Event.PROPERTY_REMOVED == type || Event.PROPERTY_CHANGED == type || Event.PROPERTY_ADDED == type) {
                replicatingPath = replicatingPath.substring(0, replicatingPath.lastIndexOf('/'));
            }
            distributionRequest = new DistributionRequest(Event.NODE_REMOVED ==
                    type ? DistributionActionType.DELETE : DistributionActionType.ADD, replicatingPath);
            log.info("distributing {}", distributionRequest);
        }
        return distributionRequest;
    }
}