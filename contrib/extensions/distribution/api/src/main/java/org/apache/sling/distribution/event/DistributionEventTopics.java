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

import aQute.bnd.annotation.ProviderType;

/**
 * an interface containing of the possible topics of events related to distribution
 */
@ProviderType
public interface DistributionEventTopics {

    public static final String EVENT_BASE = "org/apache/sling/distribution";

    /**
     * event for package created
     */
    public static final String AGENT_PACKAGE_CREATED = EVENT_BASE + "/agent/package/created";

    /**
     * event for package queued
     */
    public static final String AGENT_PACKAGE_QUEUED = EVENT_BASE + "/agent/package/queued";

    /**
     * event for package distributed
     */
    public static final String AGENT_PACKAGE_DISTRIBUTED = EVENT_BASE + "/agent/package/distributed";


    /**
     * event for package imported
     */
    public static final String IMPORTER_PACKAGE_IMPORTED = EVENT_BASE + "/importer/package/imported";
}
