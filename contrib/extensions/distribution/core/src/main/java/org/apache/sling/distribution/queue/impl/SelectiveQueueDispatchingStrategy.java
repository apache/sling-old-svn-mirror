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
package org.apache.sling.distribution.queue.impl;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueueException;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class SelectiveQueueDispatchingStrategy implements DistributionQueueDispatchingStrategy {

    private final Map<String, String> selectors;
    private final List<String> mainQueues;
    private final Map<String, String> selectorQueues;
    private final List<String> allQueues = new ArrayList<String>();

    public SelectiveQueueDispatchingStrategy(Map<String, String> selectors, String[] queueNames) {

        this.selectors = selectors;
        this.mainQueues = Arrays.asList(queueNames);
        this.selectorQueues = getMatchingQueues(null);
        this.allQueues.addAll(mainQueues);
        this.allQueues.addAll(selectorQueues.keySet());
    }

    @Override
    public Iterable<DistributionQueueItemStatus> add(@Nonnull DistributionPackage distributionPackage, @Nonnull DistributionQueueProvider queueProvider) throws DistributionQueueException {
        String[] paths = distributionPackage.getInfo().getPaths();
        Map<String, String> matchingQueues = paths != null ?  getMatchingQueues(paths) : new HashMap<String, String>();

        DistributionQueueDispatchingStrategy dispatchingStrategy = null;
        if (matchingQueues.size() > 0) {
            dispatchingStrategy = new MultipleQueueDispatchingStrategy(matchingQueues.keySet().toArray(new String[0]));
        } else {
            dispatchingStrategy = new MultipleQueueDispatchingStrategy(mainQueues.toArray(new String[0]));;
        }

        return dispatchingStrategy.add(distributionPackage, queueProvider);
    }

    @Nonnull
    @Override
    public List<String> getQueueNames() {
        return allQueues;
    }


    public Map<String, String> getMatchingQueues(String[] paths) {

        Map<String, String> result = new TreeMap<String, String>();

        if (paths == null) {
            paths = new String[] { null };
        }

        for (String queueSelector : selectors.keySet()) {
            String pathMatcher = selectors.get(queueSelector);
            int idx =  queueSelector.indexOf('|');

            String queuePrefix = queueSelector;
            String queueMatcher = null;
            if (idx >=0) {
                queuePrefix = queueSelector.substring(0, idx);
                queueMatcher = queueSelector.substring(idx+1);
            }


            for (String path : paths) {
                if (path == null || path.matches(pathMatcher)) {

                    if (queueMatcher != null) {
                        for (String mainQueue : mainQueues) {
                            if (mainQueue.matches(queueMatcher)) {
                                result.put(queuePrefix + "-" + mainQueue, mainQueue);
                            }
                        }
                    } else {
                        result.put(queuePrefix, null);
                    }
                }
            }
        }

        return result;
    }
}
