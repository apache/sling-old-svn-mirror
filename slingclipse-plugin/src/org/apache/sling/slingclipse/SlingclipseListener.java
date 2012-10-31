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
package org.apache.sling.slingclipse;

import org.apache.sling.slingclipse.api.Repository;
import org.apache.sling.slingclipse.api.FileInfo;
import org.apache.sling.slingclipse.api.RepositoryInfo;
import org.apache.sling.slingclipse.helper.SlingclipseHelper;
import org.apache.sling.slingclipse.preferences.PreferencesMessages;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * @author asanso
 *
 */
public class SlingclipseListener implements IResourceChangeListener {

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta rootDelta = event.getDelta();
		try {
			rootDelta.accept(buildVisitor());
		} catch (CoreException e) {
			SlingclipsePlugin.getDefault().getLog().log(e.getStatus());
		}
	}

	protected IResourceDeltaVisitor buildVisitor() {
		IResourceDeltaVisitor result = new IResourceDeltaVisitor() {

			@Override
			public boolean visit(IResourceDelta delta) throws CoreException { 
				
				try {
					return visitInternal(delta);
				} catch ( RuntimeException e) {
					throw new CoreException(new Status(Status.ERROR, SlingclipsePlugin.PLUGIN_ID, "Failed visiting resource based on delta " + delta, e));
				}
			}

			private boolean visitInternal(IResourceDelta delta) {
				IPreferenceStore store = SlingclipsePlugin.getDefault().getPreferenceStore();
				
				if (!store.getBoolean(PreferencesMessages.REPOSITORY_AUTO_SYNC.getKey())){
					return true;
				}
 				
				if (delta.getFlags() == IResourceDelta.NO_CHANGE
						&& (delta.getKind() != IResourceDelta.ADDED && delta
								.getKind() != IResourceDelta.REMOVED)) {
					return true;
				}
				
				IResource resource = delta.getResource();
 				String path = resource.getLocation().toString();
				
 				if (SlingclipseHelper.isValidSlingProjectPath(path)) {
					// TODO parent name clarify
					String parentPath = resource.getParent().getFullPath().toString();
 
					RepositoryInfo repositoryInfo = new RepositoryInfo(
							store.getString(PreferencesMessages.USERNAME
									.getKey()),
							store.getString(PreferencesMessages.PASSWORD
									.getKey()),
							store.getString(PreferencesMessages.REPOSITORY_URL
									.getKey()));

					Repository repository = SlingclipsePlugin.getDefault().getRepository();

					FileInfo fileInfo = new FileInfo(path,SlingclipseHelper.getSlingProjectPath(parentPath),resource.getName());

					repository.setRepositoryInfo(repositoryInfo);
					
					if (delta.getKind() == IResourceDelta.REMOVED) {
						repository.deleteNode(fileInfo);
					} else {
						repository.addNode(fileInfo);
					}

				}
 				return true;
			}
			 
		};
		return result;
	}
}
