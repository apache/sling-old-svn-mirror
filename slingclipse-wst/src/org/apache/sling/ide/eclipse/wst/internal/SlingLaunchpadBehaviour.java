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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

public class SlingLaunchpadBehaviour extends ServerBehaviourDelegate {

    @Override
    public void stop(boolean force) {
        // TODO stub
        setServerState(IServer.STATE_STOPPED);
    }

    public void start() {
        // TODO stub
        setServerState(IServer.STATE_STARTED);
    }

    // TODO refine signature, visibility
    protected void setupLaunch(ILaunch launch, String launchMode, IProgressMonitor monitor) throws CoreException {
        // TODO check that ports are free

        setServerRestartState(false);
        setServerState(IServer.STATE_STARTING);
        setMode(launchMode);
    }

    @Override
    public IStatus canPublish() {
        IStatus canPublish = super.canPublish();
        System.out.println("SlingLaunchpadBehaviour.canPublish() is " + canPublish);
        return canPublish;
    }

    @Override
    public boolean canPublishModule(IModule[] module) {
        System.out.println("SlingLaunchpadBehaviour.canPublishModule()");
        return super.canPublishModule(module);
    }

    @Override
    protected void publishServer(int kind, IProgressMonitor monitor) throws CoreException {
        System.out.println("SlingLaunchpadBehaviour.publishServer()");
        super.publishServer(kind, monitor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.wst.server.core.model.ServerBehaviourDelegate#setupLaunchConfiguration(org.eclipse.debug.core.
     * ILaunchConfigurationWorkingCopy, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor)
            throws CoreException {
        System.out.println("SlingLaunchpadBehaviour.setupLaunchConfiguration()");
        super.setupLaunchConfiguration(workingCopy, monitor);
    }
}
