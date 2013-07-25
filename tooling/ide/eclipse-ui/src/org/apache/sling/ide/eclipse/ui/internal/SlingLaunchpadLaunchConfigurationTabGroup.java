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
package org.apache.sling.ide.eclipse.ui.internal;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.wst.server.ui.ServerLaunchConfigurationTab;

public class SlingLaunchpadLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    private static final String LAUNCHPAD_SERVER_ID = "org.apache.sling.ide.launchpadServer";

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {

        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[6];
        tabs[0] = new ServerLaunchConfigurationTab(new String[] { LAUNCHPAD_SERVER_ID });
        tabs[0].setLaunchConfigurationDialog(dialog);
        tabs[1] = new JavaArgumentsTab();
        tabs[1].setLaunchConfigurationDialog(dialog);
        tabs[2] = new JavaClasspathTab();
        tabs[2].setLaunchConfigurationDialog(dialog);
        tabs[3] = new SourceLookupTab();
        tabs[3].setLaunchConfigurationDialog(dialog);
        tabs[4] = new EnvironmentTab();
        tabs[4].setLaunchConfigurationDialog(dialog);
        tabs[5] = new CommonTab();
        tabs[5].setLaunchConfigurationDialog(dialog);
        setTabs(tabs);
    }

}
