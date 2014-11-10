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

/**
 * an enum of the possible types of events related to distribution
 */
public enum DistributionEventType {

    /**
     * event for package created
     */
    PACKAGE_CREATED,

    /**
     * event for package queued
     */
    PACKAGE_QUEUED,

    /**
     * event for package replicated
     */
    PACKAGE_DISTRIBUTED,

    /**
     * event for package installed
     */
    PACKAGE_INSTALLED,

    /**
     * event for package imported
     */
    PACKAGE_IMPORTED,

    /**
     * event for agent created
     */
    AGENT_CREATED,

    /**
     * event for agent modified
     */
    AGENT_MODIFIED,

    /**
     * event for agent deleted
     */
    AGENT_DELETED;

    /**
     * common event topic base for distribution events
     */
    public static final String EVENT_TOPIC = "org/apache/sling/distribution/event";

    /**
     * get the event topic for this event type
     *
     * @return the event topic
     */
    public String getTopic() {
        return EVENT_TOPIC + "/" + name();
    }

}
