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
package org.apache.sling.engine.impl;

import java.util.Random;

import javax.management.NotCompliantMBeanException;

import junit.framework.TestCase;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

public class RequestProcessorMBeanImplTest extends TestCase {

    /**
     * Asserts that the simple standard deviation algorithm used by the
     * RequestProcessorMBeanImpl is equivalent to the Commons Math
     * SummaryStatistics implementation
     *
     * @throws NotCompliantMBeanException not expected
     */
    public void test_statistics() throws NotCompliantMBeanException {
        final SummaryStatistics mathStats = new SummaryStatistics();
        final RequestProcessorMBeanImpl bean = new RequestProcessorMBeanImpl();

        assertEquals(0l, bean.getRequestsCount());
        assertEquals(Long.MAX_VALUE, bean.getMinRequestDurationMsec());
        assertEquals(0l, bean.getMaxRequestDurationMsec());
        assertEquals(0.0, bean.getMeanRequestDurationMsec());
        assertEquals(0.0, bean.getStandardDeviationDurationMsec());

        final Random random = new Random(System.currentTimeMillis() / 17);
        final int num = 10000;
        final int min = 85;
        final int max = 250;
        for (int i = 0; i < num; i++) {
            final long value = min + random.nextInt(max - min);
            mathStats.addValue(value);
            bean.addRequestData(value);
        }

        TestCase.assertEquals("Number of points must be the same", mathStats.getN(), bean.getRequestsCount());
        TestCase.assertEquals("Min must be equal", (long) mathStats.getMin(), bean.getMinRequestDurationMsec());
        TestCase.assertEquals("Max must be equal", (long) mathStats.getMax(), bean.getMaxRequestDurationMsec());
        assertAlmostEqual("Mean", mathStats.getMean(), bean.getMeanRequestDurationMsec(), num);
        assertAlmostEqual("Standard Deviation", mathStats.getStandardDeviation(),
            bean.getStandardDeviationDurationMsec(), num);
    }

    private void assertAlmostEqual(final String message, final double v1, final double v2, int samples) {
        final double centi = v1 / samples;
        if (v2 < (v1 - centi) || v2 > (v1 + centi)) {
            TestCase.fail(message + " (expected: " + v2 + " in (" + (v1 - centi) + "," + (v1 + centi) + "))");
        }
    }
}
