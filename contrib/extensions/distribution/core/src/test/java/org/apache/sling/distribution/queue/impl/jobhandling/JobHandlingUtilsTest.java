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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.junit.Test;

/**
 * Testcase for {@link JobHandlingUtils}
 */
public class JobHandlingUtilsTest {
    @Test
    public void testFullPropertiesFromPackageCreation() throws Exception {
        DistributionPackageInfo packageInfo = new DistributionPackageInfo("vlt");
        packageInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[]{"/foo"});
        packageInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);
        packageInfo.put(DistributionPackageInfo.PROPERTY_PACKAGE_TYPE, "vlt");

        DistributionQueueItem queueItem = new DistributionQueueItem("an-id", packageInfo);

        Map<String, Object> fullPropertiesFromPackage = JobHandlingUtils.createFullProperties(queueItem);
        assertNotNull(fullPropertiesFromPackage);
        assertEquals(4, fullPropertiesFromPackage.size());
        assertNotNull(fullPropertiesFromPackage.get("distribution.request.paths"));
        assertNotNull(fullPropertiesFromPackage.get("distribution.item.id"));
        assertNotNull(fullPropertiesFromPackage.get("distribution.package.type"));
        assertNotNull(fullPropertiesFromPackage.get("distribution.request.type"));
    }
}
