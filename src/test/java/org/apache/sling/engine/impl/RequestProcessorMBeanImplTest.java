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

import static org.junit.Assert.*;

import java.util.Random;

import javax.management.NotCompliantMBeanException;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.sling.engine.impl.request.RequestData;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class RequestProcessorMBeanImplTest {
    
    private Mockery context = new JUnit4Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    /**
     * Asserts that the simple standard deviation algorithm used by the
     * RequestProcessorMBeanImpl is equivalent to the Commons Math
     * SummaryStatistics implementation.
     * 
     * It also tests that resetStatistics method, actually resets all the statistics
     *
     * @throws NotCompliantMBeanException not expected
     */
    @Test
    public void test_statistics() throws NotCompliantMBeanException {
        final SummaryStatistics durationStats = new SummaryStatistics();
        final SummaryStatistics servletCallCountStats = new SummaryStatistics();
        final SummaryStatistics peakRecursionDepthStats = new SummaryStatistics();
        final RequestProcessorMBeanImpl bean = new RequestProcessorMBeanImpl();

        assertEquals(0l, bean.getRequestsCount());
        assertEquals(Long.MAX_VALUE, bean.getMinRequestDurationMsec());
        assertEquals(0l, bean.getMaxRequestDurationMsec());
        assertEquals(0.0, bean.getMeanRequestDurationMsec(), 0);
        assertEquals(0.0, bean.getStandardDeviationDurationMsec(), 0);

        assertEquals(Integer.MAX_VALUE, bean.getMinServletCallCount());
        assertEquals(0l, bean.getMaxServletCallCount());
        assertEquals(0.0, bean.getMeanServletCallCount(), 0);
        assertEquals(0.0, bean.getStandardDeviationServletCallCount(), 0);

        assertEquals(Integer.MAX_VALUE, bean.getMinPeakRecursionDepth());
        assertEquals(0l, bean.getMaxPeakRecursionDepth());
        assertEquals(0.0, bean.getMeanPeakRecursionDepth(), 0);
        assertEquals(0.0, bean.getStandardDeviationPeakRecursionDepth(), 0);

        final Random random = new Random(System.currentTimeMillis() / 17);
        final int num = 10000;
        final int min = 85;
        final int max = 250;
        for (int i = 0; i < num; i++) {
            final long durationValue = min + random.nextInt(max - min);
            final int callCountValue = min + random.nextInt(max - min);
            final int peakRecursionDepthValue = min + random.nextInt(max - min);
            durationStats.addValue(durationValue);
            servletCallCountStats.addValue(callCountValue);
            peakRecursionDepthStats.addValue(peakRecursionDepthValue);
            
            final RequestData requestData = context.mock(RequestData.class, "requestData" + i);
            context.checking(new Expectations() {{
                one(requestData).getElapsedTimeMsec();
                will(returnValue(durationValue));
                
                one(requestData).getServletCallCount();
                will(returnValue(callCountValue));
                
                one(requestData).getPeakRecusionDepth();
                will(returnValue(peakRecursionDepthValue));
            }});
            
            
            bean.addRequestData(requestData);
        }

        assertEquals("Number of points must be the same", durationStats.getN(), bean.getRequestsCount());
        
        assertEquals("Min Duration must be equal", (long) durationStats.getMin(), bean.getMinRequestDurationMsec());
        assertEquals("Max Duration must be equal", (long) durationStats.getMax(), bean.getMaxRequestDurationMsec());
        assertAlmostEqual("Mean Duration", durationStats.getMean(), bean.getMeanRequestDurationMsec(), num);
        assertAlmostEqual("Standard Deviation Duration", durationStats.getStandardDeviation(),
            bean.getStandardDeviationDurationMsec(), num);
        
        assertEquals("Min Servlet Call Count must be equal", (long) servletCallCountStats.getMin(), bean.getMinServletCallCount());
        assertEquals("Max Servlet Call Count must be equal", (long) servletCallCountStats.getMax(), bean.getMaxServletCallCount());
        assertAlmostEqual("Mean Servlet Call Count", servletCallCountStats.getMean(), bean.getMeanServletCallCount(), num);
        assertAlmostEqual("Standard Deviation Servlet Call Count", servletCallCountStats.getStandardDeviation(),
            bean.getStandardDeviationServletCallCount(), num);
        
        assertEquals("Min Peak Recursion Depth must be equal", (long) peakRecursionDepthStats.getMin(), bean.getMinPeakRecursionDepth());
        assertEquals("Max Peak Recursion Depth must be equal", (long) peakRecursionDepthStats.getMax(), bean.getMaxPeakRecursionDepth());
        assertAlmostEqual("Mean Peak Recursion Depth", peakRecursionDepthStats.getMean(), bean.getMeanPeakRecursionDepth(), num);
        assertAlmostEqual("Standard Deviation Peak Recursion Depth", peakRecursionDepthStats.getStandardDeviation(),
            bean.getStandardDeviationPeakRecursionDepth(), num);
        
        //check method resetStatistics 
        //In the previous test, some requests have been processed, now we reset the statistics so everything statistic is reinitialized
        bean.resetStatistics();
        
        //Simulate a single request 
        final long durationValue = min + random.nextInt(max - min);
        final int callCountValue = min + random.nextInt(max - min);
        final int peakRecursionDepthValue = min + random.nextInt(max - min);
        
        final RequestData requestData = context.mock(RequestData.class, "requestDataAfterReset");
        context.checking(new Expectations() {{
            one(requestData).getElapsedTimeMsec();
            will(returnValue(durationValue));
            
            one(requestData).getServletCallCount();
            will(returnValue(callCountValue));
            
            one(requestData).getPeakRecusionDepth();
            will(returnValue(peakRecursionDepthValue));
        }});
            
            
        bean.addRequestData(requestData);
        
        //As only one request has been simulated since resetStatiscts: min, max and mean statistics should be equals to the request data
        assertEquals("After resetStatistics Number of requests must be one",1, bean.getRequestsCount());
        assertEquals("After resetStatistics Min Duration must be equal", bean.getMinRequestDurationMsec(), (long) durationValue);
        assertEquals("After resetStatistics Max Duration must be equal", bean.getMaxRequestDurationMsec(), (long) durationValue);
        assertEquals("After resetStatistics Mean Duration must be equal",  bean.getMeanRequestDurationMsec(),(double) durationValue, 0d);

        
        assertEquals("After resetStatistics Min Servlet Call Count must be equal",bean.getMinServletCallCount(), callCountValue);
        assertEquals("After resetStatistics Max Servlet Call Count must be equal",bean.getMaxServletCallCount(), callCountValue);
        assertEquals("After resetStatistics Mean Servlet Call Count", bean.getMeanServletCallCount(), (double)callCountValue, 0d);
        
        assertEquals("After resetStatistics Min Peak Recursion Depth must be equal", bean.getMinPeakRecursionDepth(),peakRecursionDepthValue );
        assertEquals("After resetStatistics Max Peak Recursion Depth must be equal", bean.getMinPeakRecursionDepth(), peakRecursionDepthValue);
        assertEquals("After resetStatistics Mean Peak Recursion Depth", bean.getMeanPeakRecursionDepth(), (double)peakRecursionDepthValue, 0d);
    }

    private void assertAlmostEqual(final String message, final double v1, final double v2, int samples) {
        final double centi = v1 / samples;
        if (v2 < (v1 - centi) || v2 > (v1 + centi)) {
            fail(message + " (expected: " + v2 + " in (" + (v1 - centi) + "," + (v1 + centi) + "))");
        }
    }
}
