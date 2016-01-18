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

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.serialization.impl.vlt.VltUtils;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JCR observation based {@link org.apache.sling.distribution.trigger.DistributionTrigger}.
 */
public class JcrEventDistributionTrigger extends AbstractJcrEventTrigger implements DistributionTrigger {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final String[] ignoredPathsPatterns;

    public JcrEventDistributionTrigger(SlingRepository repository, Scheduler scheduler, ResourceResolverFactory resolverFactory, String path, String serviceName, String[] ignoredPathsPatterns) {
        super(repository, scheduler, resolverFactory, path, serviceName);
        this.ignoredPathsPatterns = ignoredPathsPatterns;
    }

    @Override
    protected DistributionRequest processEvent(Event event) throws RepositoryException {
        DistributionRequest distributionRequest = null;
        String eventPath = event.getPath();
        String replicatingPath = getNodePathFromEvent(event);
        if (!isIgnoredPath(replicatingPath)) {

            if (VltUtils.findParent(replicatingPath, "rep:policy") != null) {
                // distribute all policies
                replicatingPath = VltUtils.findParent(replicatingPath, "rep:policy") + "/rep:policy";

                distributionRequest = new SimpleDistributionRequest(DistributionRequestType.ADD, replicatingPath);
            } else if (VltUtils.findParent(replicatingPath, "rep:membersList") != null || eventPath.endsWith("/rep:members")) {
                // group member list structure is an implementation detail and it is safer to distribute the entire group.

                String groupPath = VltUtils.findParent(replicatingPath, "rep:membersList");
                if (groupPath != null) {
                    replicatingPath = groupPath;
                }

                distributionRequest = new SimpleDistributionRequest(DistributionRequestType.ADD, true, replicatingPath);
            } else {
                distributionRequest = new SimpleDistributionRequest(Event.NODE_REMOVED == event.getType() ?
                        DistributionRequestType.DELETE : DistributionRequestType.ADD, replicatingPath);
            }

            log.info("distributing {}", distributionRequest);

        }
        return distributionRequest;
    }


    private boolean isIgnoredPath(String path) {
        if (path == null) {
            return true;
        }

        if (ignoredPathsPatterns == null || ignoredPathsPatterns.length == 0) {
            return false;
        }

        for (String pattern : ignoredPathsPatterns) {
            if (path.matches(pattern)) {
                return true;
            }
        }

        return false;
    }
}