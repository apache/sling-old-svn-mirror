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

package org.apache.sling.distribution.queue.impl;


import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.apache.sling.distribution.serialization.impl.SharedDistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PriorityQueueDispatchingStrategyTest {

    static String[] queueNames;
    static Map<String, String> selectors;

    @BeforeClass
    public static void setup() {
        queueNames = new String[] { "publish1", "publish2"};
        selectors = new HashMap<String, String>();

        selectors.put("news|publish.*", "/content/news.*");
        selectors.put("notes|publish1", "/content/notes");
        selectors.put("important", "/content/important");
    }

    @Test
    public void testQueueExpansion() throws Exception {
        PriorityQueueDispatchingStrategy dispatchingStrategy = new PriorityQueueDispatchingStrategy(selectors, queueNames);

        List<String> queues = dispatchingStrategy.getQueueNames();

        assertEquals(7, queues.size());
        assertTrue(queues.contains("publish1"));
        assertTrue(queues.contains("news-publish1"));
        assertTrue(queues.contains("publish2"));
        assertTrue(queues.contains("news-publish2"));
        assertTrue(queues.contains("notes-publish1"));
        assertTrue(queues.contains("important-publish1"));
        assertTrue(queues.contains("important-publish2"));

    }



    @Test
    public void testQueueMatching() throws Exception {
        PriorityQueueDispatchingStrategy dispatchingStrategy = new PriorityQueueDispatchingStrategy(selectors, queueNames);

        Map<String, String> matchedQueues = dispatchingStrategy.getMatchingQueues(null);

        assertEquals(5, matchedQueues.size());
        assertEquals("publish1", matchedQueues.get("news-publish1"));
        assertEquals("publish1", matchedQueues.get("notes-publish1"));
        assertEquals("publish2", matchedQueues.get("news-publish2"));
        assertEquals("publish1", matchedQueues.get("important-publish1"));
        assertEquals("publish2", matchedQueues.get("important-publish2"));
    }


    @Test
    public void testPathQueueMatching() throws Exception {
        PriorityQueueDispatchingStrategy dispatchingStrategy = new PriorityQueueDispatchingStrategy(selectors, queueNames);

        Map<String, String> matchedQueues = dispatchingStrategy.getMatchingQueues(new String[] { "/content/news/a" });

        assertEquals(2, matchedQueues.size());
        assertEquals("publish1", matchedQueues.get("news-publish1"));
        assertEquals("publish2", matchedQueues.get("news-publish2"));


        matchedQueues = dispatchingStrategy.getMatchingQueues(new String[] { "/content/notes" });

        assertEquals(1, matchedQueues.size());
        assertEquals("publish1", matchedQueues.get("notes-publish1"));

        matchedQueues = dispatchingStrategy.getMatchingQueues(new String[] { "/content/notes/a" });
        assertEquals(0, matchedQueues.size());

        matchedQueues = dispatchingStrategy.getMatchingQueues(new String[] { "/content/other" });
        assertEquals(0, matchedQueues.size());

        matchedQueues = dispatchingStrategy.getMatchingQueues(new String[] { "/content/important" });
        assertEquals(2, matchedQueues.size());
        assertEquals("publish1", matchedQueues.get("important-publish1"));
        assertEquals("publish2", matchedQueues.get("important-publish2"));

    }



    @Test
    public void testMatchingDispatching() throws Exception {
        PriorityQueueDispatchingStrategy dispatchingStrategy = new PriorityQueueDispatchingStrategy(selectors, queueNames);


        DistributionPackage distributionPackage = mock(SharedDistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[] { "/content/news/a" });
        when(distributionPackage.getInfo()).thenReturn(new DistributionPackageInfo("vlt", properties));

        DistributionQueue news1 = mock(DistributionQueue.class);
        when(news1.getName()).thenReturn("news-publish1");
        when(queueProvider.getQueue("news-publish1")).thenReturn(news1);

        DistributionQueue news2 = mock(DistributionQueue.class);
        when(news2.getName()).thenReturn("news-publish2");
        when(queueProvider.getQueue("news-publish2")).thenReturn(news2);

        dispatchingStrategy.add(distributionPackage, queueProvider);

        verify(queueProvider).getQueue("news-publish1");
        verify(queueProvider).getQueue("news-publish2");
        verifyNoMoreInteractions(queueProvider);
    }

    @Test
    public void testNoMatchingDispatching() throws Exception {
        PriorityQueueDispatchingStrategy dispatchingStrategy = new PriorityQueueDispatchingStrategy(selectors, queueNames);


        DistributionPackage distributionPackage = mock(SharedDistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[] { "/content/other" });
        when(distributionPackage.getInfo()).thenReturn(new DistributionPackageInfo("vlt", properties));

        DistributionQueue other1 = mock(DistributionQueue.class);
        when(other1.getName()).thenReturn("publish1");
        when(queueProvider.getQueue("publish1")).thenReturn(other1);

        DistributionQueue other2 = mock(DistributionQueue.class);
        when(other2.getName()).thenReturn("publish2");
        when(queueProvider.getQueue("publish2")).thenReturn(other2);

        dispatchingStrategy.add(distributionPackage, queueProvider);

        verify(queueProvider).getQueue("publish1");
        verify(queueProvider).getQueue("publish2");
        verifyNoMoreInteractions(queueProvider);
    }
}
