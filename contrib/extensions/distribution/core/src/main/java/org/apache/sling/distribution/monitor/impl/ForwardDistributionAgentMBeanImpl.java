/*/*
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
package org.apache.sling.distribution.monitor.impl;

import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.DistributionAgent;

/**
 * The ForwardDistributionAgent MBean implementation, based on the OSGi configuration.
 */
public final class ForwardDistributionAgentMBeanImpl implements ForwardDistributionAgentMBean {

    private final DistributionAgent agent;

    private final Map<String, Object> osgiConfiguration;

    public ForwardDistributionAgentMBeanImpl(DistributionAgent agent, Map<String, Object> osgiConfiguration) {
        this.agent = agent;
        this.osgiConfiguration = osgiConfiguration;
    }

    @Override
    public String getName() {
        return PropertiesUtil.toString(osgiConfiguration.get("name"), null);
    }

    @Override
    public String getTitle() {
        return PropertiesUtil.toString(osgiConfiguration.get("title"), null);
    }

    @Override
    public String getDetails() {
        return PropertiesUtil.toString(osgiConfiguration.get("details"), null);
    }

    @Override
    public boolean isEnabled() {
        return PropertiesUtil.toBoolean(osgiConfiguration.get("enabled"), true);
    }

    @Override
    public String getServiceName() {
        return PropertiesUtil.toString(osgiConfiguration.get("serviceName"), null);
    }

    @Override
    public String getLogLevel() {
        return PropertiesUtil.toString(osgiConfiguration.get("log.level"), "info");
    }

    @Override
    public String getAllowedRoots() {
        return PropertiesUtil.toString(osgiConfiguration.get("allowed.roots"), null);
    }

    @Override
    public boolean isQueueProcessingEnabled() {
        return PropertiesUtil.toBoolean(osgiConfiguration.get("queue.processing.enabled"), true);
    }

    @Override
    public String getPackageImporterEndpoints() {
        return PropertiesUtil.toString(osgiConfiguration.get("packageImporter.endpoints"), null);
    }

    @Override
    public String getPassiveQueues() {
        return PropertiesUtil.toString(osgiConfiguration.get("passiveQueues"), null);
    }

    @Override
    public String getPriorityQueues() {
        return PropertiesUtil.toString(osgiConfiguration.get("priorityQueues"), null);
    }

    @Override
    public String getRetryStrategy() {
        return PropertiesUtil.toString(osgiConfiguration.get("retry.strategy"), "none");
    }

    @Override
    public int getRetryAttempts() {
        return PropertiesUtil.toInteger(osgiConfiguration.get("retry.attempts"), 100);
    }

    @Override
    public String getQueueProvider() {
        return PropertiesUtil.toString(osgiConfiguration.get("queue.provider"), "jobs");
    }

    @Override
    public boolean isAsyncDelivery() {
        return PropertiesUtil.toBoolean(osgiConfiguration.get("async.delivery"), false);
    }


    @Override
    public String getStatus() {
        return agent.getState().name().toLowerCase();
    }
}
