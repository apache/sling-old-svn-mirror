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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import org.apache.sling.api.resource.query.QueryInstructions;
import org.apache.sling.spi.resource.provider.ProviderContext;
import org.apache.sling.spi.resource.provider.QueryProvider;
import org.apache.sling.spi.resource.provider.ResolverContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicQueryProvider implements QueryProvider<JcrProviderState> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The provider context. */
    private final ProviderContext providerContext;

    public BasicQueryProvider(final ProviderContext ctx) {
        this.providerContext = ctx;
    }

    @Override
    public org.apache.sling.spi.resource.provider.QueryResult find(final ResolverContext<JcrProviderState> ctx,
            final org.apache.sling.api.resource.query.Query q,
            final QueryInstructions qi) {

        return null;
    }
}
