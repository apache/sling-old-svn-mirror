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
package org.apache.sling.distribution.event;

import java.util.Dictionary;

import org.osgi.service.event.Event;

/**
 * A distribution related event
 */
public class DistributionEvent extends Event {

    public static final String EVENT_TOPIC = "org/apache/sling/distribution/event";

    public DistributionEvent(DistributionEventType type, Dictionary<?, ?> properties) {
        super(getTopic(type), properties);
    }

    /**
     * get the topic for a certain event type
     *
     * @param type the {@link DistributionEventType} to get the topic for
     * @return the topic for the given event type
     */
    public static String getTopic(DistributionEventType type) {
        return EVENT_TOPIC + '/' + type.name();
    }
}
