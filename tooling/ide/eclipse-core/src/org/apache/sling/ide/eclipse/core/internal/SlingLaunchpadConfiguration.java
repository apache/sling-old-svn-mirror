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
package org.apache.sling.ide.eclipse.core.internal;

import org.apache.sling.ide.eclipse.core.DefaultSlingLaunchpadConfiguration;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadConfiguration;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;


public class SlingLaunchpadConfiguration implements ISlingLaunchpadConfiguration {

    private SlingLaunchpadServer server;

    public SlingLaunchpadConfiguration(SlingLaunchpadServer server) {
        this.server = server;
    }
    
    @Override
    public boolean bundleInstallLocally() {
        return workingCopy().getAttribute(ISlingLaunchpadServer.PROP_INSTALL_LOCALLY, true);
    }

    @Override
    public int getPort() {
        return workingCopy().getAttribute(ISlingLaunchpadServer.PROP_PORT, 
                DefaultSlingLaunchpadConfiguration.INSTANCE.getPort());
    }

    @Override
    public int getDebugPort() {
        return workingCopy().getAttribute(ISlingLaunchpadServer.PROP_DEBUG_PORT, 
                DefaultSlingLaunchpadConfiguration.INSTANCE.getDebugPort());
    }
    
    private IServerWorkingCopy workingCopy() {
        IServerWorkingCopy workingCopy = server.getServerWorkingCopy();
        server.getServer().createWorkingCopy();
        if (workingCopy == null)
            workingCopy = server.getServer().createWorkingCopy();
        return workingCopy;
    }

    public void setPort(int port) {
        workingCopy().setAttribute(ISlingLaunchpadServer.PROP_PORT, port);
    }
    
    public void setDebugPort(int debugPort) {
    	workingCopy().setAttribute(ISlingLaunchpadServer.PROP_DEBUG_PORT, debugPort);
    }

    @Override
    public String getContextPath() {
        return workingCopy().getAttribute(ISlingLaunchpadServer.PROP_CONTEXT_PATH, "/");
    }

    public void setContextPath(String contextPath) {
        workingCopy().setAttribute(ISlingLaunchpadServer.PROP_CONTEXT_PATH, contextPath);
    }

    @Override
    public String getUsername() {
        return workingCopy().getAttribute(ISlingLaunchpadServer.PROP_USERNAME, 
                DefaultSlingLaunchpadConfiguration.INSTANCE.getUsername());
    }

    public void setUsername(String username) {
        workingCopy().setAttribute(ISlingLaunchpadServer.PROP_USERNAME, username);
    }

    @Override
    public String getPassword() {
        return workingCopy().getAttribute(ISlingLaunchpadServer.PROP_PASSWORD,
                DefaultSlingLaunchpadConfiguration.INSTANCE.getPassword());
    }

    public void setPassword(String password) {
        workingCopy().setAttribute(ISlingLaunchpadServer.PROP_PASSWORD, password);
    }

    @Override
    public boolean resolveSourcesInDebugMode() {
        return workingCopy().getAttribute(ISlingLaunchpadServer.PROP_RESOLVE_SOURCES, true);
    }

}
