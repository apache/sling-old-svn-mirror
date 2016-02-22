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
package org.apache.sling.ide.eclipse.sightly.ui.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;

public class NewSightlyFileWizard extends Wizard implements INewWizard {

    private IWorkbench workbench;
    private WizardNewFileCreationPage fileCreationPage;

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        
        this.workbench = workbench;
        
        fileCreationPage = new WizardNewFileCreationPage("New Sightly File", selection) {
            
            @Override
            protected InputStream getInitialContents() {
                String contents = ""
                        + "<!DOCTYPE html!>\n"
                        + "<!--/* A simple sightly script */-->\n"
                        + "<html>\n"
                        + " <head>\n"
                        + "   <title>${properties.jcr:title}</title>\n"
                        + "  </head>\n"
                        + "  <body>\n"
                        + "    <h1 data-sly-test=\"${properties.jcr:title}\">${properties.jcr:title}</h1>\n"
                        + "  </body>\n"
                        + "</html>"
                        + "";
                
                return new ByteArrayInputStream(contents.getBytes());
            }
        };
        fileCreationPage.setTitle("Sighty");
        fileCreationPage.setDescription("Create a new Sightly file");
        fileCreationPage.setImageDescriptor(SharedImages.SIGHTLY_WIZARD_BANNER);
        
        
        setWindowTitle("New Sightly File");
        addPage(fileCreationPage);
    }

    @Override
    public boolean performFinish() {
        
        if ( !fileCreationPage.isPageComplete() ) {
            return false;
        }
        
        IFile file = fileCreationPage.createNewFile();
        
        // copied from BasicNewFileResourceWizard
        // Open editor on new file.
        IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
        try {
            if (dw != null) {
                IWorkbenchPage page = dw.getActivePage();
                if (page != null) {
                    IDE.openEditor(page, file, true);
                }
            }
        } catch (PartInitException e) {
            StatusManager.getManager().handle(new Status(IStatus.WARNING, Constants.PLUGIN_ID, "Failed opening " + file + " in an editor", e), 
                    StatusManager.SHOW | StatusManager.LOG);
        }
        
        return true;
    }

}
