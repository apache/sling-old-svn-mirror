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
package org.apache.sling.startupfilterdisabler.impl;

import org.apache.sling.startupfilter.StartupFilter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public void start(BundleContext context) throws Exception {
        final ServiceReference ref = context.getServiceReference(StartupFilter.class.getName());
        if(ref == null) {
            log.warn("No StartupFilter service found to disable");
        } else {
            final StartupFilter filter = (StartupFilter)context.getService(ref);
            if(filter.isEnabled()) {
                filter.disable();
                log.info("StartupFilter disabled: {}", filter);
            } else {
                log.info("StartupFilter already disabled, nothing to do: {}", filter);
            }
        }
    }

    public void stop(BundleContext context) throws Exception {
    }
}
