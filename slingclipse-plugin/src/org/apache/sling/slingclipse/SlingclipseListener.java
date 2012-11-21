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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.slingclipse.api.Command;
import org.apache.sling.slingclipse.api.ProtectedNodes;
import org.apache.sling.slingclipse.api.Repository;
import org.apache.sling.slingclipse.api.FileInfo;
import org.apache.sling.slingclipse.api.RepositoryInfo;
import org.apache.sling.slingclipse.api.Result;
import org.apache.sling.slingclipse.helper.SlingclipseHelper;
import org.apache.sling.slingclipse.internal.SlingProjectNature;
import org.apache.sling.slingclipse.preferences.PreferencesMessages;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.json.JSONException;
import org.json.JSONML;
import org.json.JSONObject;

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

    public IResourceDeltaVisitor buildVisitor() {
		IResourceDeltaVisitor result = new IResourceDeltaVisitor() {

			@Override
			public boolean visit(IResourceDelta delta) throws CoreException { 
				
				try {
					return visitInternal(delta);
                } catch (CoreException e) {
                    throw e;
				} catch ( Exception e) {
					throw new CoreException(new Status(Status.ERROR, SlingclipsePlugin.PLUGIN_ID, "Failed visiting resource based on delta " + delta, e));
				}
			}

            private boolean visitInternal(IResourceDelta delta) throws IOException, JSONException, CoreException {
				IPreferenceStore store = SlingclipsePlugin.getDefault().getPreferenceStore();
				
				// since the listener is disabled, instruct it not to recurse for changes
				if (!store.getBoolean(PreferencesMessages.REPOSITORY_AUTO_SYNC.getKey())){
					return false;
				}
 				
				if (delta.getFlags() == IResourceDelta.NO_CHANGE
						&& (delta.getKind() != IResourceDelta.ADDED && delta
								.getKind() != IResourceDelta.REMOVED)) {
					return true;
				}
				
				IResource resource = delta.getResource();
				
                // if the project is not a Sling project skip processing and do not try to recurse
                if (!resource.getProject().getDescription().hasNature(SlingProjectNature.SLING_NATURE_ID))
                    return false;

				// don't process unhandled ( PROJECT, WORKSPACE ) resources but recurse if needed
				if ( resource.getType() != IResource.FILE && resource.getType() != IResource.FOLDER){
					return true;
				}
				
 				IPath path = resource.getLocation();
 				
 				if (SlingclipseHelper.isValidSlingProjectPath(path.toOSString())) {
					IPath parentPath = path.removeLastSegments(1);
 
					RepositoryInfo repositoryInfo = new RepositoryInfo(
							store.getString(PreferencesMessages.USERNAME
									.getKey()),
							store.getString(PreferencesMessages.PASSWORD
									.getKey()),
							store.getString(PreferencesMessages.REPOSITORY_URL
									.getKey()));

					Repository repository = SlingclipsePlugin.getDefault().getRepository();

					FileInfo fileInfo = new FileInfo(path.toOSString(), SlingclipseHelper.getSlingProjectPath(parentPath.toOSString()),resource.getName());
					
					repository.setRepositoryInfo(repositoryInfo);
					
					if (delta.getKind() == IResourceDelta.REMOVED) {
						executeCommand(repository.newDeleteNodeCommand(fileInfo));
					} else {
						addNode(repository,fileInfo);
					}

				}
 				return true;
			}
			 
		};
		return result;
	}
	
	private void addNode(Repository repository,FileInfo fileInfo) throws IOException, JSONException{
		
		Command<Void> command;
		
		if (SlingclipseHelper.CONTENT_XML.equals(fileInfo.getName())){ 
			String fileContent = readFile(fileInfo.getLocation());
			Map <String ,String>properties= getModifiedProperties(fileContent);
			command = repository.newUpdateContentNodeCommand(fileInfo, properties);
		}else{
			
			command = repository.newAddNodeCommand(fileInfo);
		}
		
		executeCommand(command);
	}

	private <T> Result<T> executeCommand(Command<T> command) {
		
		Result<T> result = command.execute();
		
		SlingclipsePlugin.getDefault().getTracer().trace("{0} : {1}.", command, result);
		
		return result;
	}
	
	private static String readFile(String path) throws IOException {
		FileInputStream stream = new FileInputStream(new File(path));
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			/* Instead of using default, pass in a decoder. */
			return Charset.defaultCharset().decode(bb).toString();
		}
		finally {
			stream.close();
		}
	}
	
	private Map <String ,String>getModifiedProperties(String fileContent) throws JSONException{
		Map<String ,String> properties= new HashMap<String ,String>();
		JSONObject json=JSONML.toJSONObject(fileContent);
		json.remove(SlingclipseHelper.TAG_NAME);
		for (Iterator<String> keys = json.keys(); keys.hasNext();) {
			String key=keys.next(); 				 
			if (!ProtectedNodes.exists(key) && !key.contains("xmlns")){
				properties.put(key, json.optString(key));			
			}
		}
		return properties;
	}
}
