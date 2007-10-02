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
package org.apache.sling.event.impl;


/**
 * Helper class defining some constants and providing support for identifying nodes in a cluster.
 */
public abstract class EventHelper {

    public static final String NODE_PROPERTY_TOPIC = "event:topic";
    public static final String NODE_PROPERTY_APPLICATION = "event:application";
    public static final String NODE_PROPERTY_CREATED = "event:created";
    public static final String NODE_PROPERTY_PROPERTIES = "event:properties";
    public static final String NODE_PROPERTY_PROCESSOR = "event:processor";
    public static final String NODE_PROPERTY_JOBID = "event:id";
    public static final String NODE_PROPERTY_ACTIVE = "event:active";
    public static final String NODE_PROPERTY_FINISHED = "event:finished";

    public static final String EVENTS_NODE_TYPE = "event:Events";
    public static final String EVENT_NODE_TYPE = "event:Event";
    public static final String JOBS_NODE_TYPE = "event:Jobs";
    public static final String JOB_NODE_TYPE = "event:Job";
    public static final String TIMED_EVENTS_NODE_TYPE = "event:TimedEvents";
    public static final String TIMED_EVENT_NODE_TYPE = "event:TimedEvent";
}
