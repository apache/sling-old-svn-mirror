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
package org.apache.sling.featureflags.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.featureflags.ClientContext;
import org.apache.sling.featureflags.ResourceHiding;
import org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;

/**
 * Resource access gate implementing the hiding of resources.
 */
@Component
@Service(value=ResourceAccessGate.class)
public class ResourceAccessImpl
    extends AllowingResourceAccessGate
    implements ResourceAccessGate {

    @Reference
    private FeatureManager manager;

    @Override
    public GateResult canRead(final Resource resource) {
        boolean available = true;
        final ClientContext info = manager.getCurrentClientContext();
        if ( info != null ) {
            for(final ResourceHiding f : ((ClientContextImpl)info).getHidingFeatures() ) {
                available = !f.hideResource(resource);
                if ( !available) {
                    break;
                }
            }
        }
        return (available ? GateResult.DONTCARE : GateResult.DENIED);
    }
}
