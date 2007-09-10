/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlet.bridge;

import org.apache.sling.launcher.servlet.SlingServlet;
import org.eclipse.equinox.http.servlet.HttpServiceServlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The <code>ServletBridge</code> class is a simple Bundle activator, which
 * registers the Eclipse Equinox <code>HttpServiceServlet</code> as the single
 * delegatee for the <code>FrameworkLauncherServlet</code>.
 * <p>
 * The Equinox <code>HttpServiceServlet</code> is a <code>javax.servlet.Servlet</code>
 * which implements the OSGi Compendium HTTP Service and as such acts as a bridge
 * between a servlet container in which the framework is running and the
 * <code>Servlet</code>s registered with the HTTP Service. 
 */
public class ServletBridge implements BundleActivator {

    private HttpServiceServlet httpService;
    
    public void start(BundleContext context) throws Exception {
        httpService = new HttpServiceServlet();
        SlingServlet.registerDelegatee(httpService);
    }
    
    
    public void stop(BundleContext context) throws Exception {
        SlingServlet.unregisterDelegatee(httpService);
        httpService = null;
    }
}
