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
package org.apache.sling.launchpad.base.webapp;

import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.felix.framework.Logger;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.launchpad.base.impl.Sling;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * The <code>SlingBridge</code> extends the base <code>Sling</code> class
 * calling the Eclipse Equinox Http Service activator for the proxy servlet to
 * be able to handle requests.
 */
public class SlingBridge extends Sling {

    public SlingBridge(Notifiable notifiable, Logger logger,
            LaunchpadContentProvider resourceProvider, Map<String, String> propOverwrite,
            ServletContext servletContext)
            throws BundleException {
        super(notifiable, logger, resourceProvider, propOverwrite);
        
        servletContext.setAttribute(BundleContext.class.getName(), getBundleContext());
    }
}
