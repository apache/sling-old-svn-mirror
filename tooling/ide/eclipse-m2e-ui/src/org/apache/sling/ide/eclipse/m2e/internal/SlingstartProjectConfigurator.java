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
package org.apache.sling.ide.eclipse.m2e.internal;

import java.util.Set;

import org.apache.sling.ide.eclipse.core.ConfigurationHelper;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class SlingstartProjectConfigurator extends AbstractProjectConfigurator {

	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
		
		IProject project = request.getProject();
		Set<String> candidates = MavenProjectUtils.getModelDirectoryCandidateLocations(request.getMavenProject());
		IPath modelsPath = null; 
		
		for ( String candidate : candidates ) {
			IPath candidatePath = Path.fromPortableString(candidate);
			IFolder modelsFolder = project.getFolder(candidatePath);
			if ( modelsFolder.exists() ) {
				modelsPath = candidatePath;
				break;
			}
		}
		
		trace("Configuring project {0} with models path {1}", project.getName(), modelsPath);
		
		ConfigurationHelper.convertToLaunchpadProject(request.getProject(), modelsPath);
	}
}
