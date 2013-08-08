/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.event.impl.jobs.jmx;

import java.util.Date;
import java.util.Dictionary;

import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.jmx.StatisticsMBean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

public class QueuesMBeanImplTest {

    private QueuesMBeanImpl mbean;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private ComponentContext componentContext;
    @Captor
    private ArgumentCaptor<String> serviceClass;
    @Captor
    private ArgumentCaptor<Object> serviceObject;
    @SuppressWarnings("rawtypes")
    @Captor
    private ArgumentCaptor<Dictionary> serviceProperties;
    @Mock
    private ServiceRegistration serviceRegistration;

    public QueuesMBeanImplTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void setup() throws NoSuchFieldException {
        mbean = new QueuesMBeanImpl();
        Mockito.when(componentContext.getBundleContext()).thenReturn(bundleContext);
        mbean.activate(componentContext.getBundleContext());
    }


    @Test
    public void testAddQueue() {
        addQueue();
    }

    public Queue addQueue() {
        Queue queue = Mockito.mock(Queue.class, Mockito.withSettings().extraInterfaces(Statistics.class));
        mockStatistics((Statistics) queue);
        Mockito.when(queue.getName()).thenReturn("queue-name");
        Mockito.when(bundleContext.registerService(Mockito.anyString(), Mockito.any(StatisticsMBean.class), Mockito.any(Dictionary.class))).thenReturn(serviceRegistration);
        mbean.sendEvent(new QueueStatusEvent(queue,null));
        Mockito.verify(bundleContext, Mockito.only()).registerService(serviceClass.capture(), serviceObject.capture(), serviceProperties.capture());
        Assert.assertEquals("Expected bean to be registerd as a StatisticsMBean ", StatisticsMBean.class.getName(), serviceClass.getValue());
        Assert.assertTrue("Expected service to be an instance of SatisticsMBean", serviceObject.getValue() instanceof StatisticsMBean);
        Assert.assertNotNull("Expected properties to have a jmx.objectname", serviceProperties.getValue().get("jmx.objectname"));
        testStatistics((StatisticsMBean) serviceObject.getValue());
        return queue;
    }


    @Test
    public void updateQueue() {
        Queue firstQueue = addQueue();
        Queue queue = Mockito.mock(Queue.class, Mockito.withSettings().extraInterfaces(Statistics.class));
        Mockito.when(queue.getName()).thenReturn("queue-name-changed");
        Mockito.reset(bundleContext);
        mbean.sendEvent(new QueueStatusEvent(queue,firstQueue));
        Mockito.verify(bundleContext, Mockito.never()).registerService(serviceClass.capture(), serviceObject.capture(), serviceProperties.capture());
    }

    @Test
    public void removeQueue() {
        Queue firstQueue = addQueue();
        mbean.sendEvent(new QueueStatusEvent(null,firstQueue));
        Mockito.verify(serviceRegistration, Mockito.only()).unregister();

    }

    private void mockStatistics(Statistics queue) {
        Mockito.when(queue.getStartTime()).thenReturn(1L);
        Mockito.when(queue.getNumberOfFinishedJobs()).thenReturn(2L);
        Mockito.when(queue.getNumberOfCancelledJobs()).thenReturn(3L);
        Mockito.when(queue.getNumberOfFailedJobs()).thenReturn(4L);
        Mockito.when(queue.getNumberOfProcessedJobs()).thenReturn(5L);
        Mockito.when(queue.getNumberOfActiveJobs()).thenReturn(6L);
        Mockito.when(queue.getNumberOfQueuedJobs()).thenReturn(7L);
        Mockito.when(queue.getNumberOfJobs()).thenReturn(8L);
        Mockito.when(queue.getLastActivatedJobTime()).thenReturn(9L);
        Mockito.when(queue.getLastFinishedJobTime()).thenReturn(10L);
        Mockito.when(queue.getAverageWaitingTime()).thenReturn(11L);
        Mockito.when(queue.getAverageProcessingTime()).thenReturn(12L);
    }

    public void testStatistics(StatisticsMBean statisticsMbean) {
        Assert.assertEquals(1, statisticsMbean.getStartTime());
        Assert.assertEquals(2, statisticsMbean.getNumberOfFinishedJobs());
        Assert.assertEquals(3, statisticsMbean.getNumberOfCancelledJobs());
        Assert.assertEquals(4, statisticsMbean.getNumberOfFailedJobs());
        Assert.assertEquals(5, statisticsMbean.getNumberOfProcessedJobs());
        Assert.assertEquals(6, statisticsMbean.getNumberOfActiveJobs());
        Assert.assertEquals(7, statisticsMbean.getNumberOfQueuedJobs());
        Assert.assertEquals(8, statisticsMbean.getNumberOfJobs());
        Assert.assertEquals(9, statisticsMbean.getLastActivatedJobTime());
        Assert.assertEquals(new Date(9), statisticsMbean.getLastActivatedJobDate());
        Assert.assertEquals(10, statisticsMbean.getLastFinishedJobTime());
        Assert.assertEquals(new Date(10), statisticsMbean.getLastFinishedJobDate());
        Assert.assertEquals(11, statisticsMbean.getAverageWaitingTime());
        Assert.assertEquals(12, statisticsMbean.getAverageProcessingTime());
    }


}
