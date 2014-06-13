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
package org.apache.sling.ide.eclipse.core.debug.impl;

import org.apache.sling.ide.eclipse.core.debug.PluginLogger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.osgi.util.NLS;

/**
 * The <tt>Tracer</tt> is the default implementation of the <tt>PluginLogger</tt>
 */
public class Tracer implements DebugOptionsListener, PluginLogger {

    private final Plugin plugin;
    private boolean debugEnabled;
    private DebugTrace trace;
    
    public Tracer(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void optionsChanged(DebugOptions options) {
    	
        String pluginId = plugin.getBundle().getSymbolicName();

        debugEnabled = options.getBooleanOption(pluginId + "/debug", false);
        trace = options.newDebugTrace(pluginId, getClass());
    }
    
    @Override
    public void trace(String message, Object... arguments) {
    	if ( !debugEnabled )
    		return;
    	
    	if ( arguments.length > 0 )
    		message = NLS.bind(message, arguments);
    	
    	trace.trace("/debug", message);
    }

    @Override
    public void trace(String message, Throwable error) {
        if (!debugEnabled)
            return;

        trace.trace("/debug", message, error);
    }

    @Override
    public void warn(String message) {
        logInternal(IStatus.WARNING, message, null);
    }

    @Override
    public void warn(String message, Throwable cause) {
        logInternal(IStatus.WARNING, message, cause);
    }

    @Override
    public void error(String message) {
        logInternal(IStatus.ERROR, message, null);
    }

    @Override
    public void error(String message, Throwable cause) {
        logInternal(IStatus.ERROR, message, cause);
    }

    private void logInternal(int statusCode, String message, Throwable cause) {
        plugin.getLog().log(new Status(statusCode, plugin.getBundle().getSymbolicName(), message, cause));
    }
}
