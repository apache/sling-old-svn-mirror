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

import org.apache.sling.junit.Activator;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Allows a test class to obtain a reference to an OSGi service. This rule embodies the logic to get a bundle context,
 * obtain a service reference, fetch the reference to the object and perform the proper cleanup after the test has run.
 */
public class Service implements TestRule {

    private final Class<?> serviceClass;

    private Object service;

    public Service(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                final BundleContext bundleContext = Activator.getBundleContext();

                if (bundleContext == null) {
                    throw new IllegalStateException("unable to obtain a bundle context");
                }

                final ServiceReference serviceReference = bundleContext.getServiceReference(serviceClass.getName());

                if (serviceReference == null) {
                    throw new IllegalStateException("unable to get a service reference");
                }

                final Object service = bundleContext.getService(serviceReference);

                if (service == null) {
                    throw new IllegalStateException("unable to get an instance of the service");
                }

                Service.this.service = serviceClass.cast(service);

                try {
                    base.evaluate();
                } finally {
                    Service.this.service = null;
                    bundleContext.ungetService(serviceReference);
                }
            }

        };
    }

    /**
     * Return the service object.
     *
     * @param serviceClass Use this class to perform a cast before the object is returned.
     * @param <T>          The type of the service.
     * @return The service object.
     */
    public <T> T getService(Class<T> serviceClass) {
        return serviceClass.cast(service);
    }

}
