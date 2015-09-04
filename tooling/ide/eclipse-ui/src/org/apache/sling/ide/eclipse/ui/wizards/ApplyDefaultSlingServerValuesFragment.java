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

import org.apache.sling.ide.eclipse.core.SlingLaunchpadConfigurationDefaults;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

/**
 * The <tt>PostProcessDefaultSlingServerValuesFragment</tt> ensures that default values are reasonable when creating a local server
 * 
 * Included checks are:
 * 
 * <ol>
 * <li><tt>auto-publish-time</tt> is set to 0</li>
 * <li>Local deployment is only enabled for hosts named <tt>localhost</tt></li>
 * </ol>
 *
 */
public class ApplyDefaultSlingServerValuesFragment extends WizardFragment {

    @Override
    public boolean hasComposite() {
        return false;
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {

        IServer server = (IServer) getTaskModel().getObject(TaskModel.TASK_SERVER);
        if (server instanceof IServerWorkingCopy) {
            IServerWorkingCopy wc = (IServerWorkingCopy) server;

            SlingLaunchpadConfigurationDefaults.applyDefaultValues(wc);
            
            wc.save(true, monitor);
        }

    }

}
