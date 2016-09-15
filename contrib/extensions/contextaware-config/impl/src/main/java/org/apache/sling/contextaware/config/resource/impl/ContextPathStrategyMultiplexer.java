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
package org.apache.sling.contextaware.config.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.contextaware.config.resource.spi.ContextPathStrategy;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Detects all {@link ContextPathStrategy} implementations in the container
 * and consolidates their result based on service ranking and root path configuration.
 */
@Component(service = ContextPathStrategyMultiplexer.class,
reference={
        @Reference(name="contextPathStrategy", service=ContextPathStrategy.class,
                bind="bindContextPathStrategy", unbind="unbindContextPathStrategy",
                cardinality=ReferenceCardinality.MULTIPLE,
                policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
})
public class ContextPathStrategyMultiplexer implements ContextPathStrategy {
    
    private static Comparator<Resource> PATH_LENGTH_COMPARATOR = new Comparator<Resource>() {
        @Override
        public int compare(Resource o1, Resource o2) {
            Integer length1 = o1.getPath().length();
            Integer length2 = o2.getPath().length();
            return length2.compareTo(length1);
        }
    };
    
    private RankedServices<ContextPathStrategy> contextPathStrategies = new RankedServices<>(Order.DESCENDING);
        
    protected void bindContextPathStrategy(ContextPathStrategy contextPathStrategy, Map<String, Object> props) {
        contextPathStrategies.bind(contextPathStrategy, props);
    }
    
    protected void unbindContextPathStrategy(ContextPathStrategy contextPathStrategy, Map<String, Object> props) {
        contextPathStrategies.unbind(contextPathStrategy, props);
    }

    @Override
    public Collection<Resource> findContextResources(Resource resource) {
        List<Collection<Resource>> allResults = getAllResults(resource);
        if (allResults.isEmpty()) {
            return Collections.emptyList();
        }
        if (allResults.size() == 1) {
            return allResults.get(0);
        }
        return mergeResults(allResults);
    }
    
    /**
     * Get all results from all registered context path strategies.
     * @param resource Start resource
     * @return List of all results
     */
    private List<Collection<Resource>> getAllResults(Resource resource) {
        List<Collection<Resource>> results = new ArrayList<>();
        for (ContextPathStrategy contextPathStrategy : contextPathStrategies) {
            Collection<Resource> result = contextPathStrategy.findContextResources(resource);
            if (!result.isEmpty()) {
                results.add(result);
            }
        }
        return results;
    }
    
    /**
     * Merges results from different context path strategy implementations.
     * Eliminating of duplicates and sorting is done solely based on path length.
     * The contract of the ContextPathStrategy defines that only partnes or the resource itself
     * is returned, so the assumption should be safe.
     * @param allResults List of all results
     * @return Merged result
     */
    private Collection<Resource> mergeResults(List<Collection<Resource>> allResults) {
        SortedSet<Resource> mergedResult = new TreeSet<>(PATH_LENGTH_COMPARATOR);
        for (Collection<Resource> resources : allResults) {
            mergedResult.addAll(resources);
        }
        return mergedResult;
    }
    
}
