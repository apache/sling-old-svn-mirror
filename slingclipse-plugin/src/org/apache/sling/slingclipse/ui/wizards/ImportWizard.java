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
package org.apache.sling.slingclipse.ui.wizards;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.sling.slingclipse.SlingclipsePlugin;
import org.apache.sling.slingclipse.api.Repository;
import org.apache.sling.slingclipse.api.RepositoryException;
import org.apache.sling.slingclipse.api.RepositoryInfo;
import org.apache.sling.slingclipse.api.ResponseType;
import org.apache.sling.slingclipse.helper.SlingclipseHelper;
import org.apache.sling.slingclipse.preferences.PreferencesMessages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.json.JSONException;
import org.json.JSONML;
import org.json.JSONObject;

/**
 * Renders the import wizard container page for the Slingclipse repository
 * import.
 */
public class ImportWizard extends Wizard implements IImportWizard {
	private ImportWizardPage mainPage;

	/**
	 * Construct a new Import Wizard container instance.
	 */
	public ImportWizard() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		
		
		if (mainPage.isPageComplete()) {

			final Repository repository = SlingclipsePlugin.getDefault().getRepository();
			RepositoryInfo repositoryInfo = new RepositoryInfo(
					mainPage.getUsername(),
					mainPage.getPassword(),
					mainPage.getRepositoryUrl());
			repository.setRepositoryInfo(repositoryInfo);
	 
			final String destinationPath = mainPage.getIntoFolderPath();
			final String repositoryPath = mainPage.getRepositoryPath();
			
			Job job = new Job("Import") {
				

				protected IStatus run(IProgressMonitor monitor) {
					monitor.setTaskName("Loading configuration...");
					monitor.worked(5);
					
					IPreferenceStore store = SlingclipsePlugin.getDefault().getPreferenceStore();
					boolean autoSync=store.getBoolean(PreferencesMessages.REPOSITORY_AUTO_SYNC.getKey());
					try {
						store.setValue(PreferencesMessages.REPOSITORY_AUTO_SYNC.getKey(), false);

						// TODO: We should try to make this give 'nice' progress feedback (aka here's what I'm processing)
						monitor.setTaskName("Importing...");
						monitor.worked(10);
				 		crawlChildrenAndImport(repository, repositoryPath,destinationPath);
						
						monitor.setTaskName("Import Complete");
						monitor.worked(100);
					} catch ( Exception e) {
						SlingclipsePlugin.getDefault().getLog().
						log(new CoreException(new Status(Status.ERROR, SlingclipsePlugin.PLUGIN_ID, "Failed importing repository ", e)).getStatus());
					}finally{
						//restore to the original value
						store.setValue(PreferencesMessages.REPOSITORY_AUTO_SYNC.getKey(), autoSync);
					}
					
					return Status.OK_STATUS;
				}
			};
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {
						System.out.println("Job Succeeded!");
					} else {
						System.err.println("Job Failed!");
					}
				}
			});
			job.setSystem(false);
			job.setUser(true);
			job.schedule();
			return true;
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 * org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Repositoy Import"); // NON-NLS-1
		setNeedsProgressMonitor(true);
		mainPage = new ImportWizardPage("Import from Repository", selection); // NON-NLS-1
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		super.addPages();
		addPage(mainPage);
	}
	
	// TODO: This probably should be pushed into the service layer
	private void crawlChildrenAndImport(Repository repository,String path,String destinationPath) throws JSONException, IOException, RepositoryException{
		String children=repository.newListChildrenNodeCommand(path,ResponseType.JSON).execute().get(); 
		JSONObject json = new JSONObject(children);
		String primaryType= json.optString(Repository.JCR_PRIMARY_TYPE);
 
		if (Repository.NT_FILE.equals(primaryType)){
			importFile(repository, path,destinationPath);
		}else if (Repository.NT_FOLDER.equals(primaryType)){
			//TODO create folder
		}else if(Repository.NT_RESOURCE.equals(primaryType)){
			//DO NOTHING
		}else{		
			createFolder(path, destinationPath);
			String content=repository.newGetNodeContentCommand(path, ResponseType.JSON).execute().get();
			JSONObject jsonContent = new JSONObject(content);
			jsonContent.put(SlingclipseHelper.TAG_NAME, Repository.JCR_ROOT);
			String contentXml = JSONML.toString(jsonContent);		
			createFile(path+"/"+SlingclipseHelper.CONTENT_XML, contentXml, destinationPath);
		}
 		
		for (Iterator<String> keys = json.keys(); keys.hasNext();) {
			String key = keys.next();
			JSONObject innerjson=json.optJSONObject(key);
			if (innerjson!=null){
				crawlChildrenAndImport(repository, path+"/"+key,destinationPath);
			}
		}
	}	
	
	private void importFile(Repository repository,String path,String destinationPath) throws JSONException, IOException, RepositoryException{ 
			byte [] node= repository.newGetNodeCommand(path).execute().get();
			createFile(path, node,destinationPath);
	}
	
	private void createFolder(String path ,String destinationPath){
		File file = new File (destinationPath+path);
		if (!file.getParentFile().exists()){
			file.getParentFile().mkdirs();
		}				
		if (!file.exists()){
			file.mkdirs();
		}			
	}
	
	private void createFile(String path, byte[] content,String destinationPath) throws IOException{		
		FileOutputStream fop = null;
		try{
			File file = new File (destinationPath+path);
			if (!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}				
			if (!file.exists()){
				file.createNewFile();
			}			
			fop = new FileOutputStream(file);
			fop.write(content);
			fop.flush();
		}finally{
			if (fop!=null){
				fop.close();
			}
		}
	}
	
	private void createFile(String path, String content,String destinationPath) throws IOException{		
		FileWriter fileWriter = null;
		try{
			File file = new File (destinationPath+path);
			if (!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}				
			if (!file.exists()){
				file.createNewFile();
			}			
            fileWriter = new FileWriter(file);
            fileWriter.write(content);
            fileWriter.close();
		}finally{
			if (fileWriter!=null){
				fileWriter.close();
			}
		}
	}
	
	

}
