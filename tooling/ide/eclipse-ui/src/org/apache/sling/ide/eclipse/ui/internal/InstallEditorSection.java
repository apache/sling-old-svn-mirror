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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.apache.sling.ide.artifacts.EmbeddedArtifact;
import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadConfiguration;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.apache.sling.ide.eclipse.core.ServerUtil;
import org.apache.sling.ide.eclipse.core.SetBundleInstallLocallyCommand;
import org.apache.sling.ide.eclipse.core.SetBundleVersionCommand;
import org.apache.sling.ide.osgi.OsgiClient;
import org.apache.sling.ide.osgi.OsgiClientException;
import org.apache.sling.ide.osgi.OsgiClientFactory;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.osgi.framework.Version;

public class InstallEditorSection extends ServerEditorSection {
    protected boolean _updating;
    protected PropertyChangeListener _listener;

    private Button bundleLocalInstallButton;
    private Button quickLocalInstallButton;
    private Hyperlink installOrUpdateSupportBundleLink;
    private ISlingLaunchpadServer launchpadServer;
    private PropertyChangeListener serverListener;
    private Label supportBundleVersionLabel;
    private Composite actionArea;
    private EmbeddedArtifactLocator artifactLocator;
    private OsgiClientFactory osgiClientFactory;

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);
        FormToolkit toolkit = getFormToolkit(parent.getDisplay());

        Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
                | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.FOCUS_TITLE);
        section.setText("Install");
        section.setDescription("Specify how to install bundles on the server");
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        // ports
        Composite composite = toolkit.createComposite(section);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        composite.setLayout(layout);
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
        composite.setLayoutData(gridData);
        toolkit.paintBordersFor(composite);
        section.setClient(composite);

        
        bundleLocalInstallButton = toolkit.createButton(composite, "Install bundles via bundle upload", SWT.RADIO);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
        bundleLocalInstallButton.setLayoutData(data);
        
        quickLocalInstallButton = toolkit.createButton(composite, "Install bundles directly from the filesystem",
                SWT.RADIO);
        data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
        quickLocalInstallButton.setLayoutData(data);

        actionArea = toolkit.createComposite(composite);
        RowLayout actionAreaLayout = new RowLayout();
        actionAreaLayout.center = true;
        actionArea.setLayout(actionAreaLayout);

        supportBundleVersionLabel = toolkit.createLabel(actionArea, "");
        installOrUpdateSupportBundleLink = toolkit.createHyperlink(actionArea, "(Install)", SWT.NONE);

        initialize();
    }

    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);

        serverListener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                if (ISlingLaunchpadServer.PROP_INSTALL_LOCALLY.equals(evt.getPropertyName())) {
            		quickLocalInstallButton.setSelection((Boolean)evt.getNewValue());
            		bundleLocalInstallButton.setSelection(!(Boolean)evt.getNewValue());
                } else if (evt.getPropertyName().equals(
                        String.format(ISlingLaunchpadServer.PROP_BUNDLE_VERSION_FORMAT,
                                EmbeddedArtifactLocator.SUPPORT_BUNDLE_SYMBOLIC_NAME))) {

                    Version launchpadVersion = new Version((String) evt.getNewValue());
                    Version embeddedVersion = new Version(artifactLocator.loadToolingSupportBundle().getVersion());

                    updateActionArea(launchpadVersion, embeddedVersion);
                }
            }
        };

        server.addPropertyChangeListener(serverListener);

        launchpadServer = (ISlingLaunchpadServer) server.getAdapter(ISlingLaunchpadServer.class);
        if (launchpadServer == null) {
            // TODO progress monitor
            launchpadServer = (ISlingLaunchpadServer) server.loadAdapter(ISlingLaunchpadServer.class,
                    new NullProgressMonitor());
        }

        artifactLocator = Activator.getDefault().getArtifactLocator();
        osgiClientFactory = Activator.getDefault().getOsgiClientFactory();
    }

    private void initialize() {

        final ISlingLaunchpadConfiguration config = launchpadServer.getConfiguration();

        quickLocalInstallButton.setSelection(config.bundleInstallLocally());
        bundleLocalInstallButton.setSelection(!config.bundleInstallLocally());

        SelectionListener listener = new SelectionAdapter() {
        	
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		execute(new SetBundleInstallLocallyCommand(server, quickLocalInstallButton.getSelection()));
        	}
		};

        quickLocalInstallButton.addSelectionListener(listener);
        bundleLocalInstallButton.addSelectionListener(listener);

        Version serverVersion = launchpadServer.getBundleVersion(EmbeddedArtifactLocator.SUPPORT_BUNDLE_SYMBOLIC_NAME);
        final EmbeddedArtifact supportBundle = artifactLocator.loadToolingSupportBundle();

        final Version embeddedVersion = new Version(supportBundle.getVersion());

        updateActionArea(serverVersion, embeddedVersion);

        installOrUpdateSupportBundleLink.addHyperlinkListener(new HyperlinkAdapter() {

            @Override
            public void linkActivated(HyperlinkEvent e) {

                ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
                dialog.setCancelable(true);
                try {
                    dialog.run(true, false, new IRunnableWithProgress() {

                        @Override
                        public void run(IProgressMonitor monitor) throws InvocationTargetException,
                                InterruptedException {
                            final Version remoteVersion;
                            monitor.beginTask("Installing support bundle", 3);
                            // double-check, just in case
                            monitor.setTaskName("Getting remote bundle version");

                            Version deployedVersion;
                            final String message;
                            try {
                                RepositoryInfo repositoryInfo = ServerUtil.getRepositoryInfo(server.getOriginal(),
                                        monitor);
                                OsgiClient client = osgiClientFactory.createOsgiClient(repositoryInfo);
                                remoteVersion = client
                                        .getBundleVersion(EmbeddedArtifactLocator.SUPPORT_BUNDLE_SYMBOLIC_NAME);
                                deployedVersion = remoteVersion;

                                monitor.worked(1);

                                if (remoteVersion != null && remoteVersion.compareTo(embeddedVersion) >= 0) {
                                    // version already up-to-date, due to bundle version
                                    // changing between startup check and now
                                    message = "Bundle is already installed and up to date";
                                } else {
                                    monitor.setTaskName("Installing bundle");
                                    
                                    try (InputStream contents = supportBundle.openInputStream() ){
                                        client.installBundle(contents, supportBundle.getName());
                                    }
                                    deployedVersion = embeddedVersion;
                                    message = "Bundle version " + embeddedVersion + " installed";

                                }
                                monitor.worked(1);

                                monitor.setTaskName("Updating server configuration");
                                final Version finalDeployedVersion = deployedVersion;
                                Display.getDefault().syncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        execute(new SetBundleVersionCommand(server,
                                                EmbeddedArtifactLocator.SUPPORT_BUNDLE_SYMBOLIC_NAME,
                                                finalDeployedVersion.toString()));
                                        try {
                                            server.save(false, new NullProgressMonitor());
                                        } catch (CoreException e) {
                                            Activator.getDefault().getLog().log(e.getStatus());
                                        }
                                    }
                                });
                                monitor.worked(1);

                            } catch (OsgiClientException | IOException | URISyntaxException e) {
                                throw new InvocationTargetException(e);
                            } finally {
                                monitor.done();
                            }

                            Display.getDefault().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    MessageDialog.openInformation(getShell(), "Support bundle install operation",
                                            message);
                                }
                            });
                        }
                    });
                } catch (InvocationTargetException e1) {

                    IStatus status = new Status(Status.ERROR, Activator.PLUGIN_ID,
                            "Error while installing support bundle: " + e1.getTargetException().getMessage(), e1
                                    .getTargetException());

                    ErrorDialog.openError(getShell(), "Error while installing support bundle", e1.getMessage(), status);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }

    private void updateActionArea(Version serverVersion, final Version embeddedVersion) {
        if (serverVersion == null || embeddedVersion.compareTo(serverVersion) > 0) {
            supportBundleVersionLabel
                    .setText("Installation support bundle is not present or outdated, deployment will not work");
            installOrUpdateSupportBundleLink.setText("(Install)");
            installOrUpdateSupportBundleLink.setEnabled(true);
        } else {
            supportBundleVersionLabel.setText("Installation support bundle is present and up to date.");
            installOrUpdateSupportBundleLink.setText("(Reinstall)");
            installOrUpdateSupportBundleLink.setEnabled(true);
        }

        actionArea.pack();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.wst.server.ui.editor.ServerEditorSection#dispose()
     */
    @Override
    public void dispose() {
        if (server != null)
            server.removePropertyChangeListener(serverListener);

        super.dispose();
    }

}
