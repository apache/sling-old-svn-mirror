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
package org.apache.sling.extensions.featureflags.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.extensions.featureflags.Feature;
import org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;

@Component
@Service(value=ResourceAccessGate.class)
public class ResourceAccessImpl
    extends AllowingResourceAccessGate
    implements ResourceAccessGate {

    @Reference
    private Feature feature;

    @Override
    public GateResult canRead(final Resource resource) {
        boolean available = true;
        final ExecutionContextFilter.ExecutionContextInfo info = ExecutionContextFilter.getCurrentExecutionContextInfo();
        if ( info != null ) {
            for(final String name : info.enabledFeatures) {
                // we can't check as Feature does not have the api (TODO - we deny for now)
                available = false;
                break;
            }
        }
        return (available ? GateResult.DONTCARE : GateResult.DENIED);
    }
}
