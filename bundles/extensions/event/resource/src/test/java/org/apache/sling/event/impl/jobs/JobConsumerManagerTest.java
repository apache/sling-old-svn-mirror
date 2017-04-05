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
package org.apache.sling.event.impl.jobs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class JobConsumerManagerTest {

    @Test public void testSimpleMappingConsumer() {
        final BundleContext bc = Mockito.mock(BundleContext.class);
        final JobConsumerManager jcs = new JobConsumerManager();
        jcs.activate(bc, Collections.EMPTY_MAP);

        final JobConsumer jc1 = Mockito.mock(JobConsumer.class);
        final ServiceReference ref1 = Mockito.mock(ServiceReference.class);
        Mockito.when(ref1.getProperty(JobConsumer.PROPERTY_TOPICS)).thenReturn("a/b");
        Mockito.when(ref1.getProperty(Constants.SERVICE_RANKING)).thenReturn(1);
        Mockito.when(ref1.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
        Mockito.when(bc.getService(ref1)).thenReturn(jc1);
        jcs.bindJobConsumer(ref1);

        assertNotNull(jcs.getExecutor("a/b"));
        assertNull(jcs.getExecutor("a"));
        assertNull(jcs.getExecutor("a/c"));
        assertNull(jcs.getExecutor("a/b/a"));
    }

    @Test public void testCategoryMappingConsumer() {
        final BundleContext bc = Mockito.mock(BundleContext.class);
        final JobConsumerManager jcs = new JobConsumerManager();
        jcs.activate(bc, Collections.EMPTY_MAP);

        final JobConsumer jc1 = Mockito.mock(JobConsumer.class);
        final ServiceReference ref1 = Mockito.mock(ServiceReference.class);
        Mockito.when(ref1.getProperty(JobConsumer.PROPERTY_TOPICS)).thenReturn("a/*");
        Mockito.when(ref1.getProperty(Constants.SERVICE_RANKING)).thenReturn(1);
        Mockito.when(ref1.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
        Mockito.when(bc.getService(ref1)).thenReturn(jc1);
        jcs.bindJobConsumer(ref1);

        assertNotNull(jcs.getExecutor("a/b"));
        assertNull(jcs.getExecutor("a"));
        assertNotNull(jcs.getExecutor("a/c"));
        assertNull(jcs.getExecutor("a/b/a"));
    }

    @Test public void testSubCategoryMappingConsumer() {
        final BundleContext bc = Mockito.mock(BundleContext.class);
        final JobConsumerManager jcs = new JobConsumerManager();
        jcs.activate(bc, Collections.EMPTY_MAP);

        final JobConsumer jc1 = Mockito.mock(JobConsumer.class);
        final ServiceReference ref1 = Mockito.mock(ServiceReference.class);
        Mockito.when(ref1.getProperty(JobConsumer.PROPERTY_TOPICS)).thenReturn("a/**");
        Mockito.when(ref1.getProperty(Constants.SERVICE_RANKING)).thenReturn(1);
        Mockito.when(ref1.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
        Mockito.when(bc.getService(ref1)).thenReturn(jc1);
        jcs.bindJobConsumer(ref1);

        assertNotNull(jcs.getExecutor("a/b"));
        assertNull(jcs.getExecutor("a"));
        assertNotNull(jcs.getExecutor("a/c"));
        assertNotNull(jcs.getExecutor("a/b/a"));
    }

    @Test public void testSimpleMappingExecutor() {
        final BundleContext bc = Mockito.mock(BundleContext.class);
        final JobConsumerManager jcs = new JobConsumerManager();
        jcs.activate(bc, Collections.EMPTY_MAP);

        final JobExecutor jc1 = Mockito.mock(JobExecutor.class);
        final ServiceReference ref1 = Mockito.mock(ServiceReference.class);
        Mockito.when(ref1.getProperty(JobConsumer.PROPERTY_TOPICS)).thenReturn("a/b");
        Mockito.when(ref1.getProperty(Constants.SERVICE_RANKING)).thenReturn(1);
        Mockito.when(ref1.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
        Mockito.when(bc.getService(ref1)).thenReturn(jc1);
        jcs.bindJobExecutor(ref1);

        assertNotNull(jcs.getExecutor("a/b"));
        assertNull(jcs.getExecutor("a"));
        assertNull(jcs.getExecutor("a/c"));
        assertNull(jcs.getExecutor("a/b/a"));
    }

    @Test public void testCategoryMappingExecutor() {
        final BundleContext bc = Mockito.mock(BundleContext.class);
        final JobConsumerManager jcs = new JobConsumerManager();
        jcs.activate(bc, Collections.EMPTY_MAP);

        final JobExecutor jc1 = Mockito.mock(JobExecutor.class);
        final ServiceReference ref1 = Mockito.mock(ServiceReference.class);
        Mockito.when(ref1.getProperty(JobExecutor.PROPERTY_TOPICS)).thenReturn("a/*");
        Mockito.when(ref1.getProperty(Constants.SERVICE_RANKING)).thenReturn(1);
        Mockito.when(ref1.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
        Mockito.when(bc.getService(ref1)).thenReturn(jc1);
        jcs.bindJobExecutor(ref1);

        assertNotNull(jcs.getExecutor("a/b"));
        assertNull(jcs.getExecutor("a"));
        assertNotNull(jcs.getExecutor("a/c"));
        assertNull(jcs.getExecutor("a/b/a"));
    }

    @Test public void testSubCategoryMappingExecutor() {
        final BundleContext bc = Mockito.mock(BundleContext.class);
        final JobConsumerManager jcs = new JobConsumerManager();
        jcs.activate(bc, Collections.EMPTY_MAP);

        final JobExecutor jc1 = Mockito.mock(JobExecutor.class);
        final ServiceReference ref1 = Mockito.mock(ServiceReference.class);
        Mockito.when(ref1.getProperty(JobExecutor.PROPERTY_TOPICS)).thenReturn("a/**");
        Mockito.when(ref1.getProperty(Constants.SERVICE_RANKING)).thenReturn(1);
        Mockito.when(ref1.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
        Mockito.when(bc.getService(ref1)).thenReturn(jc1);
        jcs.bindJobExecutor(ref1);

        assertNotNull(jcs.getExecutor("a/b"));
        assertNull(jcs.getExecutor("a"));
        assertNotNull(jcs.getExecutor("a/c"));
        assertNotNull(jcs.getExecutor("a/b/a"));
    }

    @Test public void testRanking() {
        final BundleContext bc = Mockito.mock(BundleContext.class);
        final JobConsumerManager jcs = new JobConsumerManager();
        jcs.activate(bc, Collections.EMPTY_MAP);

        final JobExecutor jc1 = Mockito.mock(JobExecutor.class);
        final JobExecutor jc2 = Mockito.mock(JobExecutor.class);
        final JobExecutor jc3 = Mockito.mock(JobExecutor.class);
        final JobExecutor jc4 = Mockito.mock(JobExecutor.class);
        final ServiceReference ref1 = Mockito.mock(ServiceReference.class);
        Mockito.when(ref1.getProperty(JobExecutor.PROPERTY_TOPICS)).thenReturn("a/b");
        Mockito.when(ref1.getProperty(Constants.SERVICE_RANKING)).thenReturn(1);
        Mockito.when(ref1.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
        Mockito.when(bc.getService(ref1)).thenReturn(jc1);
        jcs.bindJobExecutor(ref1);
        assertEquals(jc1, jcs.getExecutor("a/b"));

        final ServiceReference ref2 = Mockito.mock(ServiceReference.class);
        Mockito.when(ref2.getProperty(JobExecutor.PROPERTY_TOPICS)).thenReturn("a/b");
        Mockito.when(ref2.getProperty(Constants.SERVICE_RANKING)).thenReturn(10);
        Mockito.when(ref2.getProperty(Constants.SERVICE_ID)).thenReturn(2L);
        Mockito.when(bc.getService(ref2)).thenReturn(jc2);
        jcs.bindJobExecutor(ref2);
        assertEquals(jc2, jcs.getExecutor("a/b"));

        final ServiceReference ref3 = Mockito.mock(ServiceReference.class);
        Mockito.when(ref3.getProperty(JobExecutor.PROPERTY_TOPICS)).thenReturn("a/b");
        Mockito.when(ref3.getProperty(Constants.SERVICE_RANKING)).thenReturn(5);
        Mockito.when(ref3.getProperty(Constants.SERVICE_ID)).thenReturn(3L);
        Mockito.when(bc.getService(ref3)).thenReturn(jc3);
        jcs.bindJobExecutor(ref3);
        assertEquals(jc2, jcs.getExecutor("a/b"));

        final ServiceReference ref4 = Mockito.mock(ServiceReference.class);
        Mockito.when(ref4.getProperty(JobExecutor.PROPERTY_TOPICS)).thenReturn("a/b");
        Mockito.when(ref4.getProperty(Constants.SERVICE_RANKING)).thenReturn(5);
        Mockito.when(ref4.getProperty(Constants.SERVICE_ID)).thenReturn(4L);
        Mockito.when(bc.getService(ref4)).thenReturn(jc4);
        jcs.bindJobExecutor(ref4);
        assertEquals(jc2, jcs.getExecutor("a/b"));

        jcs.unbindJobExecutor(ref2);
        assertEquals(jc3, jcs.getExecutor("a/b"));

        jcs.unbindJobExecutor(ref3);
        assertEquals(jc4, jcs.getExecutor("a/b"));
    }
}
