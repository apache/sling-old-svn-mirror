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
package org.apache.sling.api.resource.query;

import javax.annotation.CheckForNull;

import org.apache.sling.api.resource.Resource;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface Result extends Iterable<Resource> {

    /**
     * Get a continuation key to be used with {@link QueryInstructionsBuilder#continueAt(String)}.
     * The key can be used for paging.
     * A continuation key can only be generated if the result is sorted.
     * @return A continuation key for the next resource after this search result or {@code null}.
     */
    @CheckForNull String getContinuationKey();
}
