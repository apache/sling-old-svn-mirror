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
package org.apache.sling.distribution.serialization.impl.vlt;

import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.jackrabbit.vault.fs.api.PathFilter;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.junit.Test;

import static org.apache.sling.distribution.DistributionRequestType.*;
import static org.junit.Assert.*;

public class VltUtilsTest {

    @Test
    public void testEmptyFilter() throws Exception {
        DistributionRequest request = new SimpleDistributionRequest(ADD, true, "/foo");
        NavigableMap<String, List<String>> nodeFilters =
                new TreeMap<String, List<String>>();
        NavigableMap<String, List<String>> propFilters =
                new TreeMap<String, List<String>>();
        WorkspaceFilter filter = VltUtils.createFilter(request, nodeFilters, propFilters);

        assertNotNull(filter);

        assertNotNull(filter.getPropertyFilterSets());
        List<PathFilterSet> propFilterSet = filter.getPropertyFilterSets();
        assertEquals(1, propFilterSet.size());
        PathFilterSet propFilter = propFilterSet.get(0);
        assertTrue(propFilter.getEntries().isEmpty());
        assertEquals("/", propFilter.getRoot());

        assertNotNull(filter.getFilterSets());
        List<PathFilterSet> nodeFilterSet = filter.getFilterSets();
        assertEquals(1, nodeFilterSet.size());
        PathFilterSet nodeFilter = nodeFilterSet.get(0);
        assertTrue(nodeFilter.getEntries().isEmpty());
        assertEquals("/foo", nodeFilter.getRoot());

    }

    @Test
    public void testDeepFilter() throws Exception {
        DistributionRequest request = new SimpleDistributionRequest(ADD, true, "/foo");
        NavigableMap<String, List<String>> nodeFilters =
                new TreeMap<String, List<String>>();
        nodeFilters.put("/foo", Arrays.asList("/foo/bar", "/foo/bar1"));
        NavigableMap<String, List<String>> propFilters =
                new TreeMap<String, List<String>>();
        propFilters.put("/", Arrays.asList("^.*/prop1", "^.*/prop2"));
        WorkspaceFilter wsFilter = VltUtils.createFilter(request, nodeFilters, propFilters);

        assertNotNull(wsFilter);

        assertNotNull(wsFilter.getPropertyFilterSets());
        List<PathFilterSet> propFilterSet = wsFilter.getPropertyFilterSets();
        assertEquals(1, propFilterSet.size());
        PathFilterSet propFilter = propFilterSet.get(0);
        assertEquals(2, propFilter.getEntries().size());
        PathFilter filter = propFilter.getEntries().get(0).getFilter();
        assertTrue(filter.matches("/foo/bar/prop1"));
        assertTrue(filter.matches("/foo/prop1"));
    }
}