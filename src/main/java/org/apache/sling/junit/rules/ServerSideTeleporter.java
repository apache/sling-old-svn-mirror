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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/** Server-side variant of the TeleporterRule, which provides
 *  access to OSGi services for convenience, but does not do
 *  much more.
 */
class ServerSideTeleporter extends TeleporterRule {
    private final List<ServiceReference> toUnget = new ArrayList<ServiceReference>();
    private final BundleContext bundleContext;
    
    ServerSideTeleporter() {
        bundleContext = Activator.getBundleContext();
        if(bundleContext == null) {
            throw new IllegalStateException("Null BundleContext, should not happen when this class is used");
        }
    }
    
    @Override
    protected void after() {
        super.after();
        for(ServiceReference r : toUnget) {
            if(r != null) {
                bundleContext.ungetService(r);
            }
        }
    }

    public <T> T getService (Class<T> serviceClass, String ldapFilter) {
        final ServiceGetter sg = new ServiceGetter(bundleContext, serviceClass, ldapFilter);
        toUnget.add(sg.serviceReference);
        return serviceClass.cast(sg.service);
    }
}
