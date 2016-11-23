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
package org.apache.sling.distribution.serialization;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link DistributionExportFilter}
 */
public class DistributionExportFilterTest {

    @Test
    public void testCreateFilter() throws Exception {
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/a", "/b");
        NavigableMap<String, List<String>> nodeFilters = new TreeMap<String, List<String>>();
        NavigableMap<String, List<String>> propertyFilters = new TreeMap<String, List<String>>();
        DistributionExportFilter filter = DistributionExportFilter.createFilter(request, nodeFilters, propertyFilters);
        assertNotNull(filter);
        assertNotNull(filter.getNodeFilters());
        assertNotNull(filter.getPropertyFilter());
    }
}