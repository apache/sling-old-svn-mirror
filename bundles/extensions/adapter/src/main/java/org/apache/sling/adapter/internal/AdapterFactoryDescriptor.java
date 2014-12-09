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
package org.apache.sling.adapter.internal;

import org.apache.sling.api.adapter.AdapterFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

/**
 * The <code>AdapterFactoryDescriptor</code> is an entry in the
 * {@link AdapterFactoryDescriptorMap} conveying the list of adapter (target)
 * types and the respective {@link AdapterFactory}.
 */
public class AdapterFactoryDescriptor {

    private volatile AdapterFactory factory;

    private final String[] adapters;

    private final ServiceReference reference;

    private final ComponentContext context;

    private ServiceRegistration adaption;

    public AdapterFactoryDescriptor(
            final ComponentContext context,
            final ServiceReference reference,
            final String[] adapters) {
        this.reference = reference;
        this.context = context;
        this.adapters = adapters;
    }

    public AdapterFactory getFactory() {
        if ( factory == null ) {
            factory = (AdapterFactory) context.locateService(
                    "AdapterFactory", reference);
        }
        return factory;
    }

    public String[] getAdapters() {
        return adapters;
    }

    public ServiceRegistration getAdaption() {
        return adaption;
    }

    public void setAdaption(ServiceRegistration adaption) {
        this.adaption = adaption;
    }
}
