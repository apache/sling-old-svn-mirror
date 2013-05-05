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
package org.apache.sling.discovery.impl.topology.announcement;

import java.util.Map;

import org.apache.sling.discovery.impl.common.DefaultClusterViewImpl;
import org.apache.sling.discovery.impl.common.DefaultInstanceDescriptionImpl;

/**
 * InstanceDescription which represents an instance that was announced through
 * the topology connector - and hence is by definition a remote instance.
 */
public class IncomingInstanceDescription extends DefaultInstanceDescriptionImpl {

    public IncomingInstanceDescription(final DefaultClusterViewImpl cluster,
            final boolean isLeader, final String slingId, final Map<String, String> properties) {
        // an incoming instance can never be 'myself' -> isOwn==false
        super(cluster, isLeader, false, slingId, properties);
    }

}
