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
package org.apache.sling.testing.mock.sling.oak;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.mock.sling.spi.ResourceResolverTypeAdapter;

/**
 * Resource resolver type adapter for Jackrabbit Oak repository.
 */
public class OakMockResourceResolverAdapter implements ResourceResolverTypeAdapter {

    @Override
    public ResourceResolverFactory newResourceResolverFactory() {
        return null;
    }

    @Override
    public SlingRepository newSlingRepository() {
        return new OakMockSlingRepository();
    }

}
