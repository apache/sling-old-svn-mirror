/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.junit.rules;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.junit.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/** Server-side variant of the TeleporterRule, which provides
 *  access to OSGi services for convenience, but does not do
 *  much more.
 */
class ServerSideTeleporter extends TeleporterRule {
    private final List<ServiceGetter<?>> serviceGettersToClose = new ArrayList<ServiceGetter<?>>();
    private final BundleContext bundleContext;
    private final Bundle bundleUnderTest;

    private static final int WAITFOR_SERVICE_TIMEOUT_DEFAULT_SECONDS = 10;

    ServerSideTeleporter(Class<?> classUnderTest) {
        bundleContext = Activator.getBundleContext();
        if (bundleContext == null) {
            throw new IllegalStateException("Null BundleContext, should not happen when this class is used");
        }

        Bundle bundle = FrameworkUtil.getBundle(classUnderTest);
        if (bundle == null) {
            bundle = bundleContext.getBundle();
        }
        bundleUnderTest = bundle;
    }
    
    @Override
    protected void after() {
        super.after();
        for(ServiceGetter<?> serviceGetter : serviceGettersToClose) {
            if(serviceGetter != null) {
                serviceGetter.close();
            }
        }
    }

    /**
     * Get OSGi service - if it is not available (yet?) try again and again until the configured timeout is reached.
     */
    public <T> T getService (Class<T> serviceClass, String ldapFilter) {
        String configuredTimeout = (String)bundleUnderTest.getHeaders().get("Sling-Test-WaitForService-Timeout");
        if (configuredTimeout == null) {
            configuredTimeout = Integer.toString(WAITFOR_SERVICE_TIMEOUT_DEFAULT_SECONDS);
        }
        final long timeout = Integer.parseInt(configuredTimeout) * 1000;
        try {
            T service = getServiceInternal(serviceClass, ldapFilter, timeout);
            if (service != null) {
                return service;
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(
                    "unable to get a service reference before timeout, class=" + serviceClass.getName() + ", filter='" + ldapFilter + "'", e);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid syntax for argument ldapFilter", e);
        }
        throw new IllegalStateException(
                "unable to get a service reference, class=" + serviceClass.getName() + ", filter='" + ldapFilter + "'");
    }

    private <T> T getServiceInternal (Class<T> serviceClass, String ldapFilter, long timeoutMs)
            throws InterruptedException, InvalidSyntaxException {
        ServiceGetter<T> serviceGetter = ServiceGetter.create(bundleContext, serviceClass, ldapFilter);
        serviceGettersToClose.add(serviceGetter);
        return serviceGetter.getService(timeoutMs);
    }
}
