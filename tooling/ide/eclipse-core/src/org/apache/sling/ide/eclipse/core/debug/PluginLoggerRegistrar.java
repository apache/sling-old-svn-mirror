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
package org.apache.sling.ide.eclipse.core.debug;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.ide.eclipse.core.debug.impl.Tracer;
import org.apache.sling.ide.log.Logger;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The <tt>PluginLoggerRegistrar</tt> registers {@link Logger} implementations for use for specific plugins
 *
 */
public class PluginLoggerRegistrar {

    /**
     * Registers a new tracer for the specified plugin
     * 
     * @param plugin the plugin to register for
     * @return the service registration
     */
    public static ServiceRegistration<Logger> register(Plugin plugin) {

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(DebugOptions.LISTENER_SYMBOLICNAME, plugin.getBundle().getSymbolicName());
        BundleContext ctx = plugin.getBundle().getBundleContext();
        
        // safe to downcast since we are registering the Tracer which implements Logger
        @SuppressWarnings("unchecked")
        ServiceRegistration<Logger> serviceRegistration = (ServiceRegistration<Logger>) ctx.registerService(new String[] { DebugOptionsListener.class.getName(), Logger.class.getName() },
                new Tracer(plugin), props);
        
        return serviceRegistration;
    }
}
