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
package org.apache.sling.ide.eclipse.wst.internal;

import org.eclipse.wst.server.core.IServerWorkingCopy;


public class SlingLaunchpadConfiguration {

    private SlingLaunchpadServer server;

    public SlingLaunchpadConfiguration(SlingLaunchpadServer server) {
        this.server = server;
    }

    public int getPort() {
        // TODO central place for setting defaults
        return workingCopy().getAttribute(SlingLaunchpadServer.PROP_PORT, 8080);
    }

    private IServerWorkingCopy workingCopy() {
        IServerWorkingCopy workingCopy = server.getServerWorkingCopy();
        server.getServer().createWorkingCopy();
        if (workingCopy == null)
            workingCopy = server.getServer().createWorkingCopy();
        return workingCopy;
    }

    public void setPort(int port) {
        workingCopy().setAttribute(SlingLaunchpadServer.PROP_PORT, port);
    }

    public String getContextPath() {
        return workingCopy().getAttribute(SlingLaunchpadServer.PROP_CONTEXT_PATH, "/");
    }

    public void setContextPath(String contextPath) {
        workingCopy().setAttribute(SlingLaunchpadServer.PROP_CONTEXT_PATH, contextPath);
    }

    public String getUsername() {
        return workingCopy().getAttribute(SlingLaunchpadServer.PROP_USERNAME, "admin");
    }

    public void setUsername(String username) {
        workingCopy().setAttribute(SlingLaunchpadServer.PROP_USERNAME, username);
    }

    public String getPassword() {
        return workingCopy().getAttribute(SlingLaunchpadServer.PROP_PASSWORD, "admin");
    }

    public void setPassword(String password) {
        workingCopy().setAttribute(SlingLaunchpadServer.PROP_PASSWORD, password);
    }

}
