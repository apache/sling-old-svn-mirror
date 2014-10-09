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
package org.apache.sling.ide.eclipse.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

/**
 * The <tt>OverridePublishIntervalFragment</tt> ensures that the publish interval is set to 0 when creating a Sling
 * Runtime
 *
 */
public class OverridePublishIntervalFragment extends WizardFragment {

    @Override
    public boolean hasComposite() {
        return false;
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {

        IServer server = (IServer) getTaskModel().getObject(TaskModel.TASK_SERVER);
        if (server instanceof IServerWorkingCopy) {
            IServerWorkingCopy wc = (IServerWorkingCopy) server;
            wc.setAttribute("auto-publish-time", 0);
            wc.save(true, monitor);
        }

    }

}
