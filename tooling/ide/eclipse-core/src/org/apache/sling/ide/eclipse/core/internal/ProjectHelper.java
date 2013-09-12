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

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class ProjectHelper {

	public static boolean isPotentialBundleProject(IProject project) {
		Model mavenModel = getMavenModel(project);
		return (mavenModel!=null && "bundle".equals(mavenModel.getPackaging()));
	}
	
	public static boolean isPotentialContentProject(IProject project) {
		Model mavenModel = ProjectHelper.getMavenModel(project);
		return (mavenModel!=null && "content-package".equals(mavenModel.getPackaging()));
	}
	
	public static boolean isBundleProject(IProject project) {
		return containsFacet(project, SlingBundleModuleFactory.SLING_BUNDLE_FACET_ID);
	}

	public static boolean isContentProject(IProject project) {
		return containsFacet(project, SlingContentModuleFactory.SLING_CONTENT_FACET_ID);
	}

	private static boolean containsFacet(IProject project, String facetId) {
		IFacetedProject facetedProject = (IFacetedProject) project.getAdapter(IFacetedProject.class);
		if (facetedProject==null ) {
			return false;
		}
		IProjectFacet facet = ProjectFacetsManager.getProjectFacet(facetId);
		return facetedProject.hasProjectFacet(facet);
	}
	
	static IJavaProject asJavaProject(IProject project) {
		return JavaCore.create(project);
	}
	
	static IJavaProject[] getAllJavaProjects() {
		IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		IJavaProject[] jps;
		try {
			jps = model.getJavaProjects();
			return jps;
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Model getMavenModel(IProject project) {
		IFile pomFile = project.getFile("pom.xml");
		if (!pomFile.exists()) {
			return null;
		}
		try {
			Model model = MavenPlugin.getMavenModelManager().readMavenModel(pomFile);
			return model;
		} catch (CoreException e) {
			// TODO proper logging
			e.printStackTrace();
			return null;
		}
	}
	
}
