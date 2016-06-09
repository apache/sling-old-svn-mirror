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

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.security.AccessControlException;

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

import com.google.common.collect.ImmutableMap;

/** Sling restriction provider implementation that supports the following restrictions:
 *
 * <ul>
 * <li>{@link #REP_SLING_RESOURCE_TYPE}: A restriction that allows to match against resource types.</li>
 * </ul>
 * 
 * Further sling restriction can be added here in future.
*/
@Component
@Service(RestrictionProvider.class)
public class SlingRestrictionProviderImpl extends AbstractRestrictionProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SlingRestrictionProviderImpl.class);

    public static final String REP_SLING_RESOURCE_TYPE = "rep:slingResourceTypes";

    public SlingRestrictionProviderImpl() {
        super(supportedRestrictions());
    }

    private static Map<String, RestrictionDefinition> supportedRestrictions() {
        RestrictionDefinition slingResourceTypes = new RestrictionDefinitionImpl(REP_SLING_RESOURCE_TYPE, Type.STRINGS, false);
        return ImmutableMap.of(slingResourceTypes.getName(), slingResourceTypes);
    }

    // ------------------------------------------------< RestrictionProvider >---

    @Nonnull
    @Override
    public RestrictionPattern getPattern(String oakPath, @Nonnull Tree tree) {
        if (oakPath != null) {
            PropertyState resourceTypes = tree.getProperty(REP_SLING_RESOURCE_TYPE);
            if (resourceTypes != null) {
                ResourceTypePattern resourceTypePattern = new ResourceTypePattern(resourceTypes.getValue(Type.STRINGS));
                LOG.debug("Returning resourceTypePattern={} in getPattern(String oakPath, @Nonnull Tree tree) ", resourceTypePattern);
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
                if (REP_SLING_RESOURCE_TYPE.equals(name)) {
                    ResourceTypePattern resourceTypePattern = new ResourceTypePattern(r.getProperty().getValue(Type.STRINGS));
                    LOG.debug(
                            "Returning resourceTypePattern={} in getPattern(@Nullable String oakPath, @Nonnull Set<Restriction> restrictions)",
                            resourceTypePattern);
                    return resourceTypePattern;
                }
            }
        }

        return RestrictionPattern.EMPTY;
    }

    @Override
    public void validateRestrictions(String oakPath, @Nonnull Tree aceTree) throws AccessControlException {
        super.validateRestrictions(oakPath, aceTree);
    }
}