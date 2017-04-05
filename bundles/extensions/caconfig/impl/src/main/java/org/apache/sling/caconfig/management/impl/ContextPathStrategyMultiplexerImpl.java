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
package org.apache.sling.caconfig.management.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.management.multiplexer.ContextPathStrategyMultiplexer;
import org.apache.sling.caconfig.resource.impl.util.ResourceEliminateDuplicatesIterator;
import org.apache.sling.caconfig.resource.impl.util.ResourcePathCollatingIterator;
import org.apache.sling.caconfig.resource.spi.ContextPathStrategy;
import org.apache.sling.caconfig.resource.spi.ContextResource;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Detects all {@link ContextPathStrategy} implementations in the container
 * and consolidates their result based on service ranking.
 */
@SuppressWarnings("deprecation")
@Component(service = { ContextPathStrategyMultiplexer.class, org.apache.sling.caconfig.management.ContextPathStrategyMultiplexer.class },
reference={
        @Reference(name="contextPathStrategy", service=ContextPathStrategy.class,
                bind="bindContextPathStrategy", unbind="unbindContextPathStrategy",
                cardinality=ReferenceCardinality.MULTIPLE,
                policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
})
public class ContextPathStrategyMultiplexerImpl implements ContextPathStrategyMultiplexer, org.apache.sling.caconfig.management.ContextPathStrategyMultiplexer {
    
    private RankedServices<ContextPathStrategy> items = new RankedServices<>(Order.DESCENDING);
        
    protected void bindContextPathStrategy(ContextPathStrategy contextPathStrategy, Map<String, Object> props) {
        items.bind(contextPathStrategy, props);
    }
    
    protected void unbindContextPathStrategy(ContextPathStrategy contextPathStrategy, Map<String, Object> props) {
        items.unbind(contextPathStrategy, props);
    }

    /**
     * Merges all results from the detected implementations into a single answer.
     */
    @Override
    public Iterator<ContextResource> findContextResources(Resource resource) {
        List<Iterator<ContextResource>> allResults = getAllResults(resource);
        if (allResults.isEmpty()) {
            return Collections.emptyIterator();
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
    private List<Iterator<ContextResource>> getAllResults(Resource resource) {
        List<Iterator<ContextResource>> results = new ArrayList<>();
        for (ContextPathStrategy item : items) {
            Iterator<ContextResource> result = item.findContextResources(resource);
            if (result.hasNext()) {
                results.add(result);
            }
        }
        return results;
    }
    
    /**
     * Merges results from different context path strategy implementations.
     * Eliminating of duplicates and sorting is done solely based on path length.
     * The contract of the ContextPathStrategy defines that only parents or the resource itself
     * is returned, so the assumption should be safe.
     * @param allResults List of all results
     * @return Merged result
     */
    @SuppressWarnings("unchecked")
    private Iterator<ContextResource> mergeResults(List<Iterator<ContextResource>> allResults) {
        return new ResourceEliminateDuplicatesIterator(
                new ResourcePathCollatingIterator(allResults)
        );
    }
    
}
