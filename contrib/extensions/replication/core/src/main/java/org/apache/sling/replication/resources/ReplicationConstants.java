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

package org.apache.sling.replication.resources;

public class ReplicationConstants {

    public static final String SUFFIX_RESOURCE_LIST = "/list";
    public static final String SUFFIX_AGENT_QUEUE = "/queue";
    public static final String SUFFIX_AGENT_QUEUE_EVENT = "/queue/event";

    public static final String  AGENT_RESOURCE_TYPE = "sling/replication/service/agent";
    public static final String  AGENT_QUEUE_RESOURCE_TYPE = AGENT_RESOURCE_TYPE + SUFFIX_AGENT_QUEUE;
    public static final String  AGENT_QUEUE_EVENT_RESOURCE_TYPE = AGENT_QUEUE_RESOURCE_TYPE + SUFFIX_AGENT_QUEUE_EVENT;
    public static final String  AGENT_ROOT_RESOURCE_TYPE = AGENT_RESOURCE_TYPE + SUFFIX_RESOURCE_LIST;

    public static final String  IMPORTER_RESOURCE_TYPE = "sling/replication/service/importer";
    public static final String  EXPORTER_RESOURCE_TYPE = "sling/replication/service/exporter";

}
