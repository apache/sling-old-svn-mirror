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

    public static final String THREAD_POOL_NAME = "SLING_EVENTING";

    public static final String EVENT_PREFIX = "slingevent:";

    public static final String NODE_PROPERTY_TOPIC = "slingevent:topic";
    public static final String NODE_PROPERTY_APPLICATION = "slingevent:application";
    public static final String NODE_PROPERTY_CREATED = "slingevent:created";
    public static final String NODE_PROPERTY_PROPERTIES = "slingevent:properties";
    public static final String NODE_PROPERTY_PROCESSOR = "slingevent:processor";
    public static final String NODE_PROPERTY_JOBID = "slingevent:id";
    public static final String NODE_PROPERTY_NODE_PATH = "slingevent:nodePath";
    public static final String NODE_PROPERTY_FINISHED = "slingevent:finished";
    public static final String NODE_PROPERTY_TE_EXPRESSION = "slingevent:expression";
    public static final String NODE_PROPERTY_TE_DATE = "slingevent:date";
    public static final String NODE_PROPERTY_TE_PERIOD = "slingevent:period";

    public static final String EVENTS_NODE_TYPE = "slingevent:Events";
    public static final String EVENT_NODE_TYPE = "slingevent:Event";
    public static final String JOBS_NODE_TYPE = "slingevent:Jobs";
    public static final String JOB_NODE_TYPE = "slingevent:Job";
    public static final String TIMED_EVENTS_NODE_TYPE = "slingevent:TimedEvents";
    public static final String TIMED_EVENT_NODE_TYPE = "slingevent:TimedEvent";

    public static final String NODETYPE_FOLDER = "sling:Folder";
}
