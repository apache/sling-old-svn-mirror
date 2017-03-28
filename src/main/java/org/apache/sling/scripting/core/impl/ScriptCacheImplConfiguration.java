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
package org.apache.sling.scripting.core.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import static org.apache.sling.scripting.core.impl.ScriptCacheImpl.DEFAULT_CACHE_SIZE;

@ObjectClassDefinition(
    name = "Apache Sling Script Cache",
    description = "The Script Cache is useful for running previously compiled scripts."
)
@interface ScriptCacheImplConfiguration {

    @AttributeDefinition(
        name = "Cache Size",
        description = "The Cache Size defines the maximum number of compiled script references that will be stored in the cache's internal map."
    )
    int org_apache_sling_scripting_cache_size() default DEFAULT_CACHE_SIZE;

    @AttributeDefinition(
        name = "Additional Extensions",
        description = "Scripts from the search paths with these extensions will also be monitored so that changes to them will clean the cache if the cache contains them."

    )
    String[] org_apache_sling_scripting_cache_additional__extensions() default {};

}
