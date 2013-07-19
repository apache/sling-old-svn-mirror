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

import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;

public class SlingLaunchpadLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {

        IServer server = ServerUtil.getServer(configuration);
        if (server == null) {
            throw new CoreException(new Status(IStatus.ERROR, "org.apache.sling.slingclipse", 0, "No server found",
                    null));
        }

        if (server.shouldPublish() && ServerCore.isAutoPublishing())
            server.publish(IServer.PUBLISH_INCREMENTAL, monitor);

        SlingLaunchpadBehaviour launchpad = (SlingLaunchpadBehaviour) server.loadAdapter(SlingLaunchpadBehaviour.class,
                monitor);

        IVMInstall vm = verifyVMInstall(configuration);

        IVMRunner runner = vm.getVMRunner(mode);
        if (runner == null)
            runner = vm.getVMRunner(ILaunchManager.RUN_MODE);

        // TODO use
        File workingDir = verifyWorkingDirectory(configuration);
        String workingDirName = null;
        if (workingDir != null)
            workingDirName = workingDir.getAbsolutePath();

        setDefaultSourceLocator(launch, configuration);

        File java = new File("/usr/bin/java"); // TODO configurable and use

        launchpad.setupLaunch(launch, mode, monitor);
        launchpad.start();
    }

}
