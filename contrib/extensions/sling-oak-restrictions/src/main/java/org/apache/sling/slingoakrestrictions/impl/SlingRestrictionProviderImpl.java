/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.slingoakrestrictions.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.AbstractRestrictionProvider;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.Restriction;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionDefinition;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionDefinitionImpl;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionPattern;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sling restriction provider implementation that supports the following restrictions:
 *
 * <ul>
 * <li>{@link #SLING_RESOURCE_TYPES}: A restriction that allows to match against resource types (matches are exact and do not include children).</li>
 * <li>{@link #SLING_RESOURCE_TYPES_WITH_DECENDANTS}: A restriction that allows to match against resource types and all sub nodes of matching resource types.</li>
 * </ul>
 * 
 * Further sling restriction can be added here in future.
*/
@Component
@Service(RestrictionProvider.class)
public class SlingRestrictionProviderImpl extends AbstractRestrictionProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SlingRestrictionProviderImpl.class);

    public static final String SLING_RESOURCE_TYPES = "sling:resourceTypes";
    public static final String SLING_RESOURCE_TYPES_WITH_DECENDANTS = "sling:resourceTypesWithDescendants";

    public SlingRestrictionProviderImpl() {
        super(supportedRestrictions());
    }

    private static Map<String, RestrictionDefinition> supportedRestrictions() {
        RestrictionDefinition slingResourceTypes = new RestrictionDefinitionImpl(SLING_RESOURCE_TYPES, Type.STRINGS, false);
        RestrictionDefinition slingResourceTypesWithChildren = new RestrictionDefinitionImpl(SLING_RESOURCE_TYPES_WITH_DECENDANTS, Type.STRINGS, false);
        Map<String, RestrictionDefinition> supportedRestrictions = new HashMap<String, RestrictionDefinition>();
        supportedRestrictions.put(slingResourceTypes.getName(), slingResourceTypes);
        supportedRestrictions.put(slingResourceTypesWithChildren.getName(), slingResourceTypesWithChildren);
        return Collections.unmodifiableMap(supportedRestrictions);
    }

    // ------------------------------------------------< RestrictionProvider >---

    @Nonnull
    @Override
    public RestrictionPattern getPattern(String oakPath, @Nonnull Tree tree) {
        if (oakPath != null) {
            PropertyState resourceTypes = tree.getProperty(SLING_RESOURCE_TYPES);
            if (resourceTypes != null) {
                ResourceTypePattern resourceTypePattern = new ResourceTypePattern(resourceTypes.getValue(Type.STRINGS), oakPath, false);
                LOG.trace("Returning resourceTypePattern={} for rep:slingResourceTypes in getPattern(String,Tree)", resourceTypePattern);
                return resourceTypePattern;
            }
            PropertyState resourceTypesWithChildren = tree.getProperty(SLING_RESOURCE_TYPES_WITH_DECENDANTS);
            if (resourceTypesWithChildren != null) {
                ResourceTypePattern resourceTypePattern = new ResourceTypePattern(resourceTypesWithChildren.getValue(Type.STRINGS), oakPath, true);
                LOG.trace("Returning resourceTypePattern={} for rep:slingResourceTypesWithChildren in getPattern(String,Tree)", resourceTypePattern);
                return resourceTypePattern;
            }            
        }
        return RestrictionPattern.EMPTY;
    }

    @Nonnull
    @Override
    public RestrictionPattern getPattern(@Nullable String oakPath, @Nonnull Set<Restriction> restrictions) {

        if (oakPath != null && !restrictions.isEmpty()) {
            for (Restriction r : restrictions) {
                String name = r.getDefinition().getName();
                if (SLING_RESOURCE_TYPES.equals(name)) {
                    ResourceTypePattern resourceTypePattern = new ResourceTypePattern(r.getProperty().getValue(Type.STRINGS), oakPath, false);
                    LOG.trace(
                            "Returning resourceTypePattern={} for rep:slingResourceTypes in getPattern(String,Set<Restriction>)",
                            resourceTypePattern);
                    return resourceTypePattern;
                } else if(SLING_RESOURCE_TYPES_WITH_DECENDANTS.equals(name)) {
                    ResourceTypePattern resourceTypePattern = new ResourceTypePattern(r.getProperty().getValue(Type.STRINGS), oakPath, true);
                    LOG.trace(
                            "Returning resourceTypePattern={} for rep:slingResourceTypesWithChildren in getPattern(String,Set<Restriction>)",
                            resourceTypePattern);
                    return resourceTypePattern;
                }
            }
        }

        return RestrictionPattern.EMPTY;
    }

}