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

import org.apache.sling.ide.log.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * Support class for facet installations
 * 
 * <p>By default, the <tt>enableValidationBuilderAndCommand</tt> is set to true</p>
 *
 */
class BaseFacetInstallDelegate implements IDelegate {
	
	private boolean enableValidationBuilderAndCommand = true;
	
	public void setEnableValidationBuilderAndCommand(boolean enableValidationBuilderAndCommand) {
		this.enableValidationBuilderAndCommand = enableValidationBuilderAndCommand;
	}

	@Override
	public void execute(IProject project, IProjectFacetVersion facetVersion, Object config, IProgressMonitor monitor)
			throws CoreException {
		
        Logger pluginLogger = Activator.getDefault().getPluginLogger();

        pluginLogger.trace("Installing facet {0} on project {1}", facetVersion, project);

        if ( enableValidationBuilderAndCommand ) {
        	new ProjectDescriptionManager(pluginLogger).enableValidationBuilderAndCommand(project, monitor);
        }
	}

}
