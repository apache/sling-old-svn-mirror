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
package org.apache.sling.commons.testing.sling;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.adapter.internal.AdapterManagerImpl;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.commons.testing.osgi.MockBundle;
import org.apache.sling.commons.testing.osgi.MockComponentContext;
import org.apache.sling.commons.testing.osgi.MockServiceReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;


/**
 * Sets up an {@link AdapterManagerImpl} in a junit testing environment. This
 * class is in the same package as the {@link AdapterManagerImpl} in order to
 * access the protected activate method.
 *
 */
public class AdapterManagerTestHelper {

    private static AdapterManagerImpl adapterMgr;

    private static MockComponentContext mockContext;

    private static List<ServiceReference> registeredFactories = new ArrayList<ServiceReference>();

    private static void initAdapterManager() {
        if (adapterMgr == null) {
            adapterMgr = new AdapterManagerImpl();

            mockContext = new MockComponentContext(new MockBundle(14));
            adapterMgr.activate(mockContext);
        }
    }

    public static void registerAdapterFactory(AdapterFactory adapterFactory,
            String[] adaptableClasses, String[] adapterClasses) {
        initAdapterManager();

        Bundle bundle = new MockBundle(1L);
        MockServiceReference ref = new MockServiceReference(bundle);
        mockContext.addService(ref, adapterFactory);
        ref.setProperty(Constants.SERVICE_ID, 1L);
        ref.setProperty(AdapterFactory.ADAPTABLE_CLASSES, adaptableClasses);
        ref.setProperty(AdapterFactory.ADAPTER_CLASSES, adapterClasses);
        adapterMgr.bindAdapterFactory(ref);

        registeredFactories.add(ref);
    }

    public static void resetAdapterFactories() {
        if (adapterMgr != null) {
            for (ServiceReference ref : registeredFactories) {
                adapterMgr.unbindAdapterFactory(ref);
            }
        }
    }
}
