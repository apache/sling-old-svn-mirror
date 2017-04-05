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
package org.apache.sling.scripting.thymeleaf.it.app;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.thymeleaf.linkbuilder.ILinkBuilder;

public class Activator implements BundleActivator {

    private ServiceRegistration linkBuilderRegistration;

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        registerLinkBuilder(bundleContext);
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
        if (linkBuilderRegistration != null) {
            linkBuilderRegistration.unregister();
            linkBuilderRegistration = null;
        }
    }

    private void registerLinkBuilder(final BundleContext bundleContext) {
        final FooBarLinkBuilder linkBuilder = new FooBarLinkBuilder();
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Scripting Thymeleaf IT FooBarLinkBuilder");
        properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        linkBuilderRegistration = bundleContext.registerService(ILinkBuilder.class, linkBuilder, properties);
    }

}
