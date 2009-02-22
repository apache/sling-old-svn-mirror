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
package org.apache.sling.jcr.resource.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * Tests for the <code>JcrResourceResolverFactoryImpl</code>
 */
@RunWith(JMock.class)
public class JcrResourceResolverFactoryImplTest {

    protected Mockery context;

    protected JcrResourceResolverFactoryImpl factory = new JcrResourceResolverFactoryImpl();

    public JcrResourceResolverFactoryImplTest() {
        this.context = new JUnit4Mockery();
    }

    protected Mockery getMockery() {
        return this.context;
    }

    @org.junit.Test public void testJcrResourceTypeProviders() {
        // component context is null, so everything is added to the delayed list
        assertTrue(factory.delayedJcrResourceTypeProviders.isEmpty());
        factory.bindJcrResourceTypeProvider(new ServiceReferenceImpl(7, 1L, null));
        assertFalse(factory.delayedJcrResourceTypeProviders.isEmpty());
        factory.bindJcrResourceTypeProvider(new ServiceReferenceImpl(8, 5L, null));
        factory.bindJcrResourceTypeProvider(new ServiceReferenceImpl(3, 2L, 100L));
        factory.bindJcrResourceTypeProvider(new ServiceReferenceImpl(5, 3L, 50L));
        factory.bindJcrResourceTypeProvider(new ServiceReferenceImpl(6, 4L, 50L));
        factory.bindJcrResourceTypeProvider(new ServiceReferenceImpl(2, 6L, 150L));
        // lets set up the compnent context
        final ComponentContext componentContext = this.getMockery().mock(ComponentContext.class);
        this.getMockery().checking(new Expectations() {{
            allowing(componentContext).locateService(with(any(String.class)), with(any(ServiceReference.class)));
            will(returnValue(null));
        }});
        assertTrue(factory.jcrResourceTypeProviders.isEmpty());
        factory.componentContext = componentContext;
        factory.bindJcrResourceTypeProvider(new ServiceReferenceImpl(4, 7L, 80L));
        assertFalse(factory.jcrResourceTypeProviders.isEmpty());
        factory.processDelayedJcrResourceTypeProviders();
        factory.bindJcrResourceTypeProvider(new ServiceReferenceImpl(1, 8L, 180L));
        assertTrue(factory.delayedJcrResourceTypeProviders.isEmpty());
        assertFalse(factory.jcrResourceTypeProviders.isEmpty());
        Assert.assertEquals(factory.jcrResourceTypeProviders.size(), 8);
        final long[] ids = {8,6,2,7,3,4,1,5};
        for(int i=0;i<8;i++) {
            Assert.assertEquals(factory.jcrResourceTypeProviders.get(i).serviceId, ids[i]);
        }
        Assert.assertEquals(factory.getJcrResourceTypeProvider().length, 8);
    }

    /**
     * Mock implementation of a service reference.
     */
    protected final class ServiceReferenceImpl implements ServiceReference {

        protected final Long serviceId;
        protected final Long ranking;
        protected final int  order;

        public ServiceReferenceImpl(int order, Long serviceId, Long ranking) {
            this.serviceId = serviceId;
            this.ranking = ranking;
            this.order = order;
        }

        public Bundle getBundle() {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getProperty(String key) {
            if ( Constants.SERVICE_ID.equals(key) ) {
                return this.serviceId;
            } else if ( Constants.SERVICE_RANKING.equals(key) ) {
                return this.ranking;
            }
            return null;
        }

        public String[] getPropertyKeys() {
            // TODO Auto-generated method stub
            return null;
        }

        public Bundle[] getUsingBundles() {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean isAssignableTo(Bundle bundle, String className) {
            // TODO Auto-generated method stub
            return false;
        }

        public int compareTo(Object reference) {
            // TODO Auto-generated method stub
            return 0;
        }
    }

}
