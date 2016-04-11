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

package org.apache.sling.distribution.resources.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.DistributionAgentState;
import org.apache.sling.distribution.component.impl.DistributionComponent;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.DistributionComponentProvider;
import org.apache.sling.distribution.log.DistributionLog;
import org.apache.sling.distribution.queue.impl.ErrorQueueDispatchingStrategy;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.apache.sling.distribution.resources.impl.common.SimplePathInfo;

/**
 * Extended service resource provider exposes children resources like .../agents/agentName/queues/queueName/queueItem
 */
public class ExtendedDistributionServiceResourceProvider extends DistributionServiceResourceProvider {

    private static final String QUEUES_PATH = "queues";
    private static final String LOG_PATH = "log";
    private static final String STATUS_PATH = "status";


    private static final int MAX_QUEUE_DEPTH = 100;


    public ExtendedDistributionServiceResourceProvider(String kind,
                                                       DistributionComponentProvider componentProvider,
                                                       String resourceRoot) {
        super(kind, componentProvider, resourceRoot);
    }


    @Override
    protected Map<String, Object> getChildResourceProperties(DistributionComponent component, String childResourceName) {
        DistributionComponentKind kind = component.getKind();
        if (kind.equals(DistributionComponentKind.AGENT)) {
            DistributionAgent agent = (DistributionAgent) component.getService();

            if (agent != null && childResourceName != null) {
                if (childResourceName.startsWith(QUEUES_PATH)) {
                    SimplePathInfo queuePathInfo = SimplePathInfo.parsePathInfo(QUEUES_PATH, childResourceName);
                    return getQueueProperties(agent, queuePathInfo);
                } else if (childResourceName.startsWith(LOG_PATH)) {
                    Map<String, Object> result = new HashMap<String, Object>();
                    result.put(SLING_RESOURCE_TYPE, DistributionResourceTypes.LOG_RESOURCE_TYPE);
                    DistributionLog distributionLog = agent.getLog();

                    result.put(INTERNAL_ADAPTABLE, distributionLog);

                    return result;
                } else if (childResourceName.startsWith(STATUS_PATH)) {
                    Map<String, Object> result = new HashMap<String, Object>();
                    DistributionAgentState agentState = agent.getState();

                    result.put("state", agentState.name());
                    return result;
                }

            }
        }
        return null;
    }

    @Override
    protected Iterable<String> getChildResourceChildren(DistributionComponent component, String childResourceName) {

        DistributionComponentKind kind = component.getKind();
        if (kind.equals(DistributionComponentKind.AGENT)) {
            DistributionAgent agent = (DistributionAgent) component.getService();

            if (agent != null) {
                if (childResourceName == null) {
                    List<String> nameList = new ArrayList<String>();
                    nameList.add(QUEUES_PATH);
                    nameList.add(LOG_PATH);
                    nameList.add(STATUS_PATH);

                    return nameList;
                }
            }
        }
        return null;
    }

    private Map<String, Object> getQueueProperties(DistributionAgent agent, SimplePathInfo queueInfo) {
        if (queueInfo.isRoot()) {
            Map<String, Object> result = new HashMap<String, Object>();

            List<String> nameList = new ArrayList<String>();
            for (String name : agent.getQueueNames()) {
                nameList.add(name);
            }
            result.put(ITEMS, nameList.toArray(new String[nameList.size()]));

            result.put(SLING_RESOURCE_TYPE, DistributionResourceTypes.AGENT_QUEUE_LIST_RESOURCE_TYPE);
            return result;
        } else if (queueInfo.isMain()) {
            String queueName = queueInfo.getMainResourceName();
            Map<String, Object> result = new HashMap<String, Object>();

            DistributionQueue queue = agent.getQueue(queueName);

            if (queue != null) {
                DistributionQueueStatus queueStatus = queue.getStatus();
                result.put(SLING_RESOURCE_TYPE, DistributionResourceTypes.AGENT_QUEUE_RESOURCE_TYPE);

                result.put("state", queueStatus.getState().name());
                result.put("empty", queueStatus.isEmpty());
                result.put("itemsCount", queueStatus.getItemsCount());

                if (queueName.startsWith(ErrorQueueDispatchingStrategy.ERROR_PREFIX)) {
                    String retryQueue = queueName.replace(ErrorQueueDispatchingStrategy.ERROR_PREFIX, "");
                    result.put("retryQueue", retryQueue);
                }

                List<String> nameList = new ArrayList<String>();
                Map<String, Map<String, Object>> propertiesMap = new HashMap<String, Map<String, Object>>();
                for (DistributionQueueEntry entry : queue.getItems(0, MAX_QUEUE_DEPTH)) {
                    nameList.add(entry.getId());
                    propertiesMap.put(entry.getId(), getItemProperties(entry));
                }

                result.put(ITEMS, nameList.toArray(new String[nameList.size()]));
                result.put(INTERNAL_ITEMS_PROPERTIES, propertiesMap);
                result.put(INTERNAL_ADAPTABLE, queue);
            }


            return result;

        } else if (queueInfo.isChild()) {
            String queueName = queueInfo.getMainResourceName();
            Map<String, Object> result = new HashMap<String, Object>();

            DistributionQueue queue = agent.getQueue(queueName);

            if (queue != null) {
                String itemId = queueInfo.getChildResourceName();

                DistributionQueueEntry entry = queue.getItem(itemId);
                result = getItemProperties(entry);
            }

            return result;
        }

        return null;
    }


    private Map<String, Object> getItemProperties(DistributionQueueEntry entry) {
        Map<String, Object> result = new HashMap<String, Object>();


        if (entry != null) {

            result.put(SLING_RESOURCE_TYPE, DistributionResourceTypes.AGENT_QUEUE_ITEM_RESOURCE_TYPE);

            DistributionQueueItem item = entry.getItem();
            DistributionPackageInfo packageInfo = DistributionPackageUtils.fromQueueItem(item);

            result.put("id", entry.getId());
            result.put("paths", packageInfo.getPaths());
            result.put("action", packageInfo.getRequestType());
            result.put("userid", packageInfo.get(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_USER, String.class));

            DistributionQueueItemStatus status = entry.getStatus();
            result.put("attempts", status.getAttempts());
            result.put("time", status.getEntered().getTime());
            result.put("state", status.getItemState().name());

        }

        return result;

    }

}
