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

import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.DistributionAgentException;
import org.apache.sling.distribution.component.impl.DistributionComponent;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.DistributionComponentProvider;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueException;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.apache.sling.distribution.resources.impl.common.SimplePathInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extended service resource provider exposes children resources like .../agents/agentName/queues/queueName/queueItem
 */
public class ExtendedDistributionServiceResourceProvider extends DistributionServiceResourceProvider {

    private static final String QUEUES_PATH = "queues";
    private static final int MAX_QUEUE_DEPTH = 100;


    public ExtendedDistributionServiceResourceProvider(String kind,
                                               DistributionComponentProvider componentProvider,
                                               String resourceRoot) {
        super(kind, componentProvider, resourceRoot);
    }


    @Override
    protected Map<String,Object> getChildResourceProperties(DistributionComponent component, String childResourceName) {
        DistributionComponentKind kind =  component.getKind();
        if (kind.equals(DistributionComponentKind.AGENT)) {
            DistributionAgent agent = (DistributionAgent) component.getService();

            if (agent != null) {
                if (childResourceName.startsWith(QUEUES_PATH)) {
                    SimplePathInfo queuePathInfo = SimplePathInfo.parsePathInfo(QUEUES_PATH, childResourceName);
                    Map<String, Object> result = getQueueProperties(agent, queuePathInfo);
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    protected Iterable<String> getChildResourceChildren(DistributionComponent component, String childResourceName) {

        DistributionComponentKind kind =  component.getKind();
        if (kind.equals(DistributionComponentKind.AGENT)) {
            DistributionAgent agent = (DistributionAgent) component.getService();

            if (agent != null) {
                if (childResourceName == null) {
                    List<String> nameList = new ArrayList<String>();
                    nameList.add(QUEUES_PATH);
                    return nameList;
                }
            }
        }
        return null;
    }

    private Map<String,Object> getQueueProperties(DistributionAgent agent, SimplePathInfo queueInfo) {
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

            try {
                DistributionQueue queue = agent.getQueue(queueName);

                result.put(SLING_RESOURCE_TYPE, DistributionResourceTypes.AGENT_QUEUE_RESOURCE_TYPE);
                result.put("state", queue.getState().name());
                result.put("empty", queue.isEmpty());
                result.put("itemsCount", queue.getItemsCount());

                List<String> nameList = new ArrayList<String>();
                for (DistributionQueueItem item : queue.getItems(0, MAX_QUEUE_DEPTH)) {
                    nameList.add(item.getId());
                }
                result.put(ITEMS, nameList.toArray(new String[0]));
                result.put(ADAPTABLE_PROPERTY_NAME, queue);

            } catch (DistributionAgentException e) {

            }

            return result;

        } else if (queueInfo.isChild()) {
            String queueName = queueInfo.getMainResourceName();
            Map<String, Object> result = new HashMap<String, Object>();

            try {
                DistributionQueue queue = agent.getQueue(queueName);
                String itemId = queueInfo.getChildResourceName();

                DistributionQueueItem item = queue.getItem(itemId);

                if (item != null) {

                    result.put(SLING_RESOURCE_TYPE, DistributionResourceTypes.AGENT_QUEUE_ITEM_RESOURCE_TYPE);
                    result.put("id", item.getId());
                    result.put("paths", item.getPackageInfo().getPaths());
                    result.put("action", item.getPackageInfo().getRequestType());
                    result.put("type", item.getType());

                    DistributionQueueItemStatus status = queue.getStatus(item);
                    result.put("attempts", status.getAttempts());
                    result.put("time", status.getEntered().getTime());
                    result.put("state", status.getItemState().name());

                }
            } catch (DistributionAgentException e) {

            } catch (DistributionQueueException e) {
            }
            return result;
        }

        return null;
    }

}
