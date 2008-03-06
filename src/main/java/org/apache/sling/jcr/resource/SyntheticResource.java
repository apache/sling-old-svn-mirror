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
package org.apache.sling.jcr.resource;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * The <code>SyntheticResource</code> class is a simple implementation of the
 * <code>Resource</code> interface which may be used to provide a resource
 * object which has no related repository item.
 * <p>
 * @deprecated to be removed soooon
 */
@Deprecated
public class SyntheticResource extends
        org.apache.sling.api.resource.SyntheticResource {

    public SyntheticResource(ResourceResolver resourceResolver, String path,
            String resourceType) {
        super(resourceResolver, path, resourceType);
    }

}
