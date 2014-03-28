/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.resourceresolver.impl.mapping;

import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolverFactory;

/**
 * Internal interface representing the additional methods
 * MapEntries needs from the ResourceResolverFactory.
 *
 * Exists primarily to facilitate mocking of the ResourceResolverFactory
 * when testing MapEntries.
 */
public interface MapConfigurationProvider extends ResourceResolverFactory {

    String getMapRoot();

    Map<?, ?> getVirtualURLMap();

    Mapping[] getMappings();

    int getDefaultVanityPathRedirectStatus();

    boolean isVanityPathEnabled();

    boolean isOptimizeAliasResolutionEnabled();

    public class VanityPathConfig implements Comparable<VanityPathConfig> {
        public final boolean isExclude;
        public final String prefix;

        public VanityPathConfig(final String prefix, final boolean isExclude) {
            this.prefix = prefix;
            this.isExclude = isExclude;
        }

        public int compareTo(VanityPathConfig o2) {
            return new Integer(o2.prefix.length()).compareTo(this.prefix.length());
        }
    }

    /**
     * A list of white and black list prefixes all ending with a slash.
     * If <code>null</code> is returned, all paths are allowed.
     */
    List<VanityPathConfig> getVanityPathConfig();
}
