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
package org.apache.sling.distribution.monitor.impl;

import static org.apache.sling.distribution.packaging.DistributionPackageInfo.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.junit.Test;

/**
 * Test case for {@link DistributionPackageMBean}
 */
public class DistributionPackageMBeanTest {

    @Test
    public void verifyMBeanExposedValues() {
        String type = "jcrvlt";
        long processingTime = 2000L;

        Map<String, Object> base = new HashMap<String, Object>();
        base.put(PROPERTY_REQUEST_PATHS, new String[]{ "a", "b", "c" });
        base.put(PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);
        DistributionPackageInfo distributionPackageInfo = new DistributionPackageInfo(type, base);

        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        when(distributionPackage.getId()).thenReturn("#distributionPackage");
        when(distributionPackage.getSize()).thenReturn(1000L);
        when(distributionPackage.getInfo()).thenReturn(distributionPackageInfo);

        DistributionPackageMBean mBean = new DistributionPackageMBeanImpl(distributionPackage,
                                                                          type,
                                                                          processingTime);

        assertEquals(distributionPackage.getId(), mBean.getId());
        assertEquals(type, mBean.getType());
        assertArrayEquals(distributionPackageInfo.getPaths(), mBean.getPaths());
        assertEquals(distributionPackageInfo.getRequestType().name().toLowerCase(), mBean.getRequestType());
        assertEquals(distributionPackage.getSize(), mBean.getSize());
        assertEquals(processingTime, mBean.getProcessingTime());
    }

}
