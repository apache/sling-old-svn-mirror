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

import java.lang.reflect.InvocationTargetException;

import org.apache.sling.ide.eclipse.core.ServerUtil;
import org.apache.sling.ide.eclipse.core.internal.ResourceChangeCommandFactory;
import org.apache.sling.ide.eclipse.ui.WhitelabelSupport;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.Result;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;

public class ExportWizard extends Wizard {

    private IResource syncStartPoint;
    private ExportWizardPage exportPage;

    public void init(IWorkbench workbench, IResource syncStartPoint) {
        setWindowTitle("Repositoy Export"); // NON-NLS-1
        setNeedsProgressMonitor(true);
        setDefaultPageImageDescriptor(WhitelabelSupport.getWizardBanner());

        this.syncStartPoint = syncStartPoint;
        this.exportPage = new ExportWizardPage(syncStartPoint);
    }

    @Override
    public void addPages() {
        addPage(exportPage);
    }

    @Override
    public boolean performFinish() {

        try {
            getContainer().run(true, false, new IRunnableWithProgress() {

                @Override
                public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    final ResourceChangeCommandFactory factory = new ResourceChangeCommandFactory(Activator
                            .getDefault().getSerializationManager());

                    final Repository[] selectedServer = new Repository[1];
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                selectedServer[0] = ServerUtil.getConnectedRepository(exportPage.getServer(), monitor);
                            } catch (CoreException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

                    try {

                        syncStartPoint.accept(new IResourceVisitor() {

                            @Override
                            public boolean visit(IResource resource) throws CoreException {
                                Command<?> command = factory.newCommandForAddedOrUpdated(selectedServer[0], resource);
                                if (command == null) {
                                    return true;
                                }
                                Result<?> result = command.execute();
                                if (!result.isSuccess()) {
                                    throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID,
                                            "Failed exporting: " + result.toString()));
                                }

                                return true;
                            }
                        });
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        throw new InvocationTargetException(t);
                    }
                }
            });

            return true;
        } catch (RuntimeException | InterruptedException e) {
            exportPage.setErrorMessage(e.getMessage());
            return false;
        } catch (InvocationTargetException e) {
            exportPage.setErrorMessage(e.getCause().getMessage());
            return false;
        }

    }
}
