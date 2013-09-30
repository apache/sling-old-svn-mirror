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
package org.apache.sling.ide.eclipse.core;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServerWorkingCopy;

public class SetBundleVersionCommand extends AbstractOperation {

    private IServerWorkingCopy server;
    private final String bundleSymbolicName;
    private final String bundleVersion;
    private String oldBundleVersion;

    public SetBundleVersionCommand(IServerWorkingCopy server, String bundleSymbolicName, String bundleVersion) {
        super("Setting bundle version...");

        this.server = server;
        this.bundleSymbolicName = bundleSymbolicName;
        this.bundleVersion = bundleVersion;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        String propertyName = propertyName();

        oldBundleVersion = server.getAttribute(propertyName, (String) null);
        server.setAttribute(propertyName, bundleVersion);

        return Status.OK_STATUS;
    }

    private String propertyName() {
        return String.format(ISlingLaunchpadServer.PROP_BUNDLE_VERSION_FORMAT, this.bundleSymbolicName);
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        return execute(monitor, info);
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        server.setAttribute(propertyName(), oldBundleVersion);

        return Status.OK_STATUS;
    }

}
