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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.discovery.impl.common.DefaultClusterViewImpl;

/**
 * InstanceDescription which is used at bootstrap time when there is no
 * established view yet - hence the instance is considered to be in 'isolated'
 * state.
 */
public class IsolatedInstanceDescription extends EstablishedInstanceDescription {

    public IsolatedInstanceDescription(final Resource res, final String clusterViewId,
            final String slingId) {
        super(null, res, slingId, true, true);

        DefaultClusterViewImpl clusterView = new DefaultClusterViewImpl(
                clusterViewId);
        clusterView.addInstanceDescription(this);
    }

}
