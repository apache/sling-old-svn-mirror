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
package org.apache.sling.discovery.impl.common.resource;

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.discovery.base.its.setup.mock.MockedResource;
import org.apache.sling.discovery.base.its.setup.mock.MockedResourceResolver;
import org.apache.sling.discovery.commons.providers.DefaultClusterView;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescription;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescriptionTest;

public class EstablishedInstanceDescriptionTest extends DefaultInstanceDescriptionTest {

    @Override
    public DefaultInstanceDescription constructInstanceDescription(
            DefaultClusterView clusterView, boolean isLeader,
            boolean isOwn, String theSlingId, Map<String, String> properties)
            throws Exception {

        Resource res = new MockedResource(new MockedResourceResolver(),
                "/foo/bar", "nt:unstructured");

        return new EstablishedInstanceDescription(clusterView, res, theSlingId,
                isLeader, isOwn);
    }

}
