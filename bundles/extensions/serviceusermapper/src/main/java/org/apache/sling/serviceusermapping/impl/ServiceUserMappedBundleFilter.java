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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;

@Component(immediate=true) // framework gets/ungets hooks each time
@Service(value = {EventListenerHook.class, FindHook.class} )
/**
 * The <code>ServiceUserMappingBundleFilter</code> only allows the bundle for which the service mapping is available to see it.
 */
public class ServiceUserMappedBundleFilter implements EventListenerHook, FindHook {

    @Override
    public void event(ServiceEvent serviceEvent, Map map) {

        ServiceReference serviceReference = serviceEvent.getServiceReference();
        if (isServiceMappingReference(serviceReference)) {
            Object serviceName = serviceReference.getProperty(Mapping.SERVICENAME);

            if (serviceName != null && serviceName instanceof String) {
                Iterator<Map.Entry<BundleContext, Collection<ListenerHook.ListenerInfo>>> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    BundleContext ctx = it.next().getKey();

                    String bundleServiceName = ServiceUserMapperImpl.getServiceName(ctx.getBundle());
                    if (!serviceName.equals(bundleServiceName)) {
                        it.remove();
                    }
                }
            }
        }
    }

    @Override
    public void find(BundleContext bundleContext, String name, String filter, boolean allServices,
                     Collection references) {
        String bundleServiceName = ServiceUserMapperImpl.getServiceName(bundleContext.getBundle());

        Iterator<ServiceReference> it = references.iterator();
        while (it.hasNext()) {
            ServiceReference serviceReference = it.next();
            if (isServiceMappingReference(serviceReference)) {
                Object serviceName = serviceReference.getProperty(Mapping.SERVICENAME);

                if (serviceName != null && !serviceName.equals(bundleServiceName)) {
                    it.remove();
                }
            }
        }
    }

    private static boolean isServiceMappingReference(ServiceReference serviceReference) {
        Object objectClass = serviceReference.getProperty(Constants.OBJECTCLASS);
        for (Object o :  (Object[]) objectClass) {
            if (ServiceUserMappedImpl.SERVICEUSERMAPPED.equals(o)) {
                return true;
            }
        }
        return false;
    }



}
