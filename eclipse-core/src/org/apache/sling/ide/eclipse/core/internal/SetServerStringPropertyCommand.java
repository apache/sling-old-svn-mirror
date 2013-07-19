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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServerWorkingCopy;

public abstract class SetServerStringPropertyCommand extends AbstractOperation {

    private IServerWorkingCopy server;
    private String newValue;
    private String propertyName;
    private String defaultValue;
    private String oldValue;

    public SetServerStringPropertyCommand(IServerWorkingCopy server, String propertyName, String newValue,
            String defaultValue) {
        super("Setting property " + propertyName + " ...");

        this.server = server;
        this.propertyName = propertyName;
        this.newValue = newValue;
        this.defaultValue = defaultValue;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        oldValue = server.getAttribute(propertyName, defaultValue);

        server.setAttribute(propertyName, newValue);

        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        return execute(monitor, info);
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        server.setAttribute(propertyName, oldValue);

        return Status.OK_STATUS;
    }

}
