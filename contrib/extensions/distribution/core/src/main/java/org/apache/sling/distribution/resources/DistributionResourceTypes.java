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
package org.apache.sling.distribution.resources;

/**
 * constants distribution resource types and paths
 */
public class DistributionResourceTypes {

    public static final String DEFAULT_SETTING_RESOURCE_TYPE = "sling/distribution/setting";
    public static final String DEFAULT_SERVICE_RESOURCE_TYPE = "sling/distribution/service";

    public static final String AGENT_RESOURCE_TYPE = "sling/distribution/service/agent";
    public static final String AGENT_LIST_RESOURCE_TYPE = "sling/distribution/service/agent/list";
    public static final String AGENT_QUEUE_RESOURCE_TYPE = "sling/distribution/service/agent/queue";
    public static final String AGENT_QUEUE_LIST_RESOURCE_TYPE = "sling/distribution/service/agent/queue/list";
    public static final String AGENT_QUEUE_ITEM_RESOURCE_TYPE = "sling/distribution/service/agent/queue/item";

    public static final String LOG_RESOURCE_TYPE = "sling/distribution/service/log";

    public static final String IMPORTER_RESOURCE_TYPE = "sling/distribution/service/importer";
    public static final String IMPORTER_LIST_RESOURCE_TYPE = "sling/distribution/service/importer/list";

    public static final String EXPORTER_RESOURCE_TYPE = "sling/distribution/service/exporter";
    public static final String EXPORTER_LIST_RESOURCE_TYPE = "sling/distribution/service/exporter/list";

    public static final String TRIGGER_RESOURCE_TYPE = "sling/distribution/service/trigger";
    public static final String TRIGGER_LIST_RESOURCE_TYPE = "sling/distribution/service/trigger/list";
}
