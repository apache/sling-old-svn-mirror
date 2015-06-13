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
package org.apache.sling.spi.resource.provider;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.query.Query;
import org.apache.sling.api.resource.query.QueryInstructions;

import aQute.bnd.annotation.ConsumerType;

@ConsumerType
public interface QueryProvider <T> {

    /**
     * Execute the query within the context of the provided resource resolver
     * @param ctx The resource context-
     * @param q The query
     * @param qi The query instructions
     * @return A query result or {@code null}.
     */
    @CheckForNull QueryResult find(@Nonnull ResolveContext<T> ctx,
            @Nonnull Query q,
            @Nonnull QueryInstructions qi);
}
