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
package org.apache.sling.distribution.queue.impl.jobhandling;

import java.util.Map;

import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link JobHandlingUtils}
 */
public class JobHandlingUtilsTest {
    @Test
    public void testFullPropertiesFromPackageCreation() throws Exception {
        DistributionQueueItem distributionQueueItem = mock(DistributionQueueItem.class);
        when(distributionQueueItem.getAction()).thenReturn("ADD");
        when(distributionQueueItem.getId()).thenReturn("an-id");
        when(distributionQueueItem.getPaths()).thenReturn(new String[]{"/content", "/apps"});
        when(distributionQueueItem.getType()).thenReturn("vlt");
        Map<String, Object> fullPropertiesFromPackage = JobHandlingUtils.createFullProperties(distributionQueueItem);
        assertNotNull(fullPropertiesFromPackage);
        assertEquals(4, fullPropertiesFromPackage.size());
        assertNotNull(fullPropertiesFromPackage.get("distribution.package.paths"));
        assertNotNull(fullPropertiesFromPackage.get("distribution.package.id"));
        assertNotNull(fullPropertiesFromPackage.get("distribution.package.type"));
        assertNotNull(fullPropertiesFromPackage.get("distribution.package.action"));
    }

    @Test
    public void testIdPropertiesFromPackageCreation() throws Exception {
        DistributionQueueItem distributionPackage = mock(DistributionQueueItem.class);
        when(distributionPackage.getId()).thenReturn("an-id");
        Map<String, Object> idPropertiesFromPackage = JobHandlingUtils.createIdProperties(distributionPackage.getId());
        assertNotNull(idPropertiesFromPackage);
        assertEquals(1, idPropertiesFromPackage.size());
        assertNotNull(idPropertiesFromPackage.get("distribution.package.id"));
    }
}
