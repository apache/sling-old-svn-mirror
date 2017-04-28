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

package org.apache.sling.serviceusermapping.impl;


import junit.framework.TestCase;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test reference and bundle filtering based on <code>Mapping.SERVICENAME</code>
 */
public class ServiceUserMappedBundleFilterTest {

    final static String BUNDLE1 = "bundle1";
    final static String BUNDLE2 = "bundle2";


    final static BundleContext bundleContext1;
    final static BundleContext bundleContext2;

    static  {
        bundleContext1 = mock(BundleContext.class);
        Bundle bundle1 = mock(Bundle.class);
        when(bundleContext1.getBundle()).thenReturn(bundle1);
        when(bundle1.getSymbolicName()).thenReturn(BUNDLE1);


        bundleContext2 = mock(BundleContext.class);
        Bundle bundle2 = mock(Bundle.class);
        when(bundleContext2.getBundle()).thenReturn(bundle2);
        when(bundle2.getSymbolicName()).thenReturn(BUNDLE2);

    }




    @Test
    public void testEvent() {
        Map<BundleContext, Collection<ListenerHook.ListenerInfo>> map = new HashMap<BundleContext, Collection<ListenerHook.ListenerInfo>>();

        map.put(bundleContext1, new ArrayList<ListenerHook.ListenerInfo>());
        map.put(bundleContext2, new ArrayList<ListenerHook.ListenerInfo>());

        ServiceEvent serviceEvent = mock(ServiceEvent.class);
        ServiceReference serviceReference = mock(ServiceReference.class);
        when(serviceEvent.getServiceReference()).thenReturn(serviceReference);
        when(serviceReference.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[]{ServiceUserMappedImpl.SERVICEUSERMAPPED});
        when(serviceReference.getProperty(Mapping.SERVICENAME)).thenReturn(BUNDLE1);


        EventListenerHook eventListenerHook = new ServiceUserMappedBundleFilter();
        eventListenerHook.event(serviceEvent, map);

        TestCase.assertEquals(1, map.size());
        TestCase.assertTrue(map.containsKey(bundleContext1));

    }

    @Test
    public void testFind() {
        List collection = new ArrayList<ServiceReference>();

        ServiceReference serviceReference1 = mock(ServiceReference.class);
        ServiceReference serviceReference2 = mock(ServiceReference.class);
        collection.add(serviceReference1);
        collection.add(serviceReference2);

        when(serviceReference1.getProperty(Mapping.SERVICENAME)).thenReturn(BUNDLE1);
        when(serviceReference1.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[]{ServiceUserMappedImpl.SERVICEUSERMAPPED});

        when(serviceReference2.getProperty(Mapping.SERVICENAME)).thenReturn(BUNDLE2);
        when(serviceReference2.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[]{ServiceUserMappedImpl.SERVICEUSERMAPPED});

        FindHook findHook = new ServiceUserMappedBundleFilter();
        findHook.find(bundleContext1, null, null, false, collection);

        TestCase.assertEquals(1, collection.size());
        TestCase.assertTrue(collection.contains(serviceReference1));
    }
}
