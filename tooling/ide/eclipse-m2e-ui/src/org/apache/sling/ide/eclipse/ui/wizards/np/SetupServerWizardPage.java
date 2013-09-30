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
package org.apache.sling.ide.eclipse.ui.wizards.np;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.apache.sling.ide.artifacts.EmbeddedArtifact;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.apache.sling.ide.eclipse.m2e.internal.Activator;
import org.apache.sling.ide.osgi.OsgiClient;
import org.apache.sling.ide.osgi.OsgiClientException;
import org.apache.sling.ide.osgi.OsgiClientFactory;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.osgi.framework.Version;

public class SetupServerWizardPage extends WizardPage {
	
    private Button useExistingServer;
	private Combo existingServerCombo;
	private Button setupNewServer;
	private Text newServerName;
	private Text newServerHostnameName;
	private Text newServerPort;
	private Text newServerDebugPort;
	private Button installToolingSupportBundle;
	
	private Map<String, IServer> serversMap = new HashMap<String, IServer>();

	public SetupServerWizardPage(AbstractNewSlingApplicationWizard parent) {
		super("chooseArchetypePage");
		setTitle("Select or Setup Launchpad Server");
		setDescription("This step defines which server to use with the new Sling application.");
		setImageDescriptor(parent.getLogo());
	}

    @Override
    public AbstractNewSlingApplicationWizard getWizard() {
        return (AbstractNewSlingApplicationWizard) super.getWizard();
    }

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;

		useExistingServer = new Button(container, SWT.RADIO);
	    GridData useExistingServerButtonData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
	    useExistingServer.setLayoutData(useExistingServerButtonData);
	    useExistingServer.setText("Add to existing server");

	    Label existingServerLabel = new Label(container, SWT.NONE);
	    GridData locationLabelData = new GridData();
	    locationLabelData.horizontalIndent = 10;
	    existingServerLabel.setLayoutData(locationLabelData);
	    existingServerLabel.setText("Location:");
	    existingServerLabel.setEnabled(true);

	    existingServerCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
	    GridData locationComboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
	    existingServerCombo.setLayoutData(locationComboData);
	    existingServerCombo.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent e) {
	    	  dialogChanged();
	      }
	    });
	    existingServerCombo.setEnabled(true);

	    {
		    setupNewServer = new Button(container, SWT.RADIO);
		    GridData setupNewServerButtonData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
		    setupNewServer.setLayoutData(setupNewServerButtonData);
		    setupNewServer.setText("Setup new server");
	    }
	    
		{
			Label newServerLabel;
		    newServerLabel = new Label(container, SWT.NONE);
		    GridData newServerLabelData = new GridData();
		    newServerLabelData.horizontalIndent = 10;
		    newServerLabel.setLayoutData(newServerLabelData);
		    newServerLabel.setText("Server name:");
		    newServerLabel.setEnabled(true);
	    }

	    {
		    newServerName = new Text(container, SWT.BORDER);
		    GridData newServerNameData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		    newServerName.setLayoutData(newServerNameData);
	    }
	    
		{
			Label newServerHostnameLabel;
		    newServerHostnameLabel = new Label(container, SWT.NONE);
		    GridData newServerHostnameLabelData = new GridData();
		    newServerHostnameLabelData.horizontalIndent = 10;
		    newServerHostnameLabel.setLayoutData(newServerHostnameLabelData);
		    newServerHostnameLabel.setText("Host name:");
		    newServerHostnameLabel.setEnabled(true);
	    }

	    {
		    newServerHostnameName = new Text(container, SWT.BORDER);
		    GridData newServerHostnameNameData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		    newServerHostnameName.setLayoutData(newServerHostnameNameData);
	    }
	    
		{
			Label newServerPortLabel;
			newServerPortLabel = new Label(container, SWT.NONE);
		    GridData newServerPortLabelData = new GridData();
		    newServerPortLabelData.horizontalIndent = 10;
		    newServerPortLabel.setLayoutData(newServerPortLabelData);
		    newServerPortLabel.setText("Port:");
		    newServerPortLabel.setEnabled(true);
	    }

	    {
		    newServerPort = new Text(container, SWT.BORDER);
		    GridData newServerPortData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		    newServerPort.setLayoutData(newServerPortData);
	    }
	    
		{
			Label newServerDebugPortLabel;
			newServerDebugPortLabel = new Label(container, SWT.NONE);
		    GridData newServerDebugPortLabelData = new GridData();
		    newServerDebugPortLabelData.horizontalIndent = 10;
		    newServerDebugPortLabel.setLayoutData(newServerDebugPortLabelData);
		    newServerDebugPortLabel.setText("Debug Port:");
		    newServerDebugPortLabel.setEnabled(true);
	    }

	    {
		    newServerDebugPort = new Text(container, SWT.BORDER);
		    GridData newServerDebugPortData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		    newServerDebugPort.setLayoutData(newServerDebugPortData);
	    }
	    
	    {
	    	installToolingSupportBundle = new Button(container, SWT.CHECK);
		    GridData installToolingSupportBundleData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		    installToolingSupportBundleData.horizontalIndent = 10;
		    installToolingSupportBundle.setLayoutData(installToolingSupportBundleData);
		    installToolingSupportBundle.setText("Check/Install org.apache.sling.tooling.support.install bundle");
		    installToolingSupportBundle.setSelection(true);
	    }
	    
	    
	    SelectionAdapter radioListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				existingServerCombo.setEnabled(useExistingServer.getSelection());
				newServerName.setEnabled(setupNewServer.getSelection());
				newServerHostnameName.setEnabled(setupNewServer.getSelection());
				newServerPort.setEnabled(setupNewServer.getSelection());
				newServerDebugPort.setEnabled(setupNewServer.getSelection());
				installToolingSupportBundle.setEnabled(setupNewServer.getSelection());
				dialogChanged();
			}
		};
		useExistingServer.addSelectionListener(radioListener);
		setupNewServer.addSelectionListener(radioListener);
	    useExistingServer.setSelection(false);
	    existingServerCombo.setEnabled(false);
	    setupNewServer.setSelection(true);
	    installToolingSupportBundle.setSelection(true);
	    
	    ModifyListener ml = new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		};
	    KeyListener kl = new KeyListener() {
			
			@Override
			public void keyReleased(KeyEvent e) {
				dialogChanged();
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				dialogChanged();
			}
		};
		newServerName.addModifyListener(ml);
		newServerName.addKeyListener(kl);
		newServerHostnameName.addModifyListener(ml);
		newServerHostnameName.addKeyListener(kl);
		newServerPort.addModifyListener(ml);
		newServerPort.addKeyListener(kl);
		newServerDebugPort.addModifyListener(ml);
		newServerDebugPort.addKeyListener(kl);
		
		initialize();
		setPageComplete(false);
		setControl(container);
	}

	private void initialize() {
		IServer[] servers = ServerCore.getServers();
		for (int i = 0; i < servers.length; i++) {
			IServer server = servers[i];
			String key = keyFor(server);
			serversMap.put(key, server);
			existingServerCombo.add(key);
		}
	}

	private String keyFor(IServer server) {
		return server.getName();
	}

	private void dialogChanged() {
		if (useExistingServer.getSelection()) {
			if (existingServerCombo.getSelectionIndex()==-1) {
				updateStatus("Choose existing server from the list");
				return;
			}
		} else if (setupNewServer.getSelection()) {
			if (newServerName.getText().length()==0 ||
					getHostname().length()==0 ||
					newServerPort.getText().length()==0 ||
					newServerDebugPort.getText().length()==0) {
				updateStatus("Enter values for new server");
				return;
			}
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	

    private Version getToolingSupportBundleVersion() throws OsgiClientException {

        return newOsgiClient().getBundleVersion(EmbeddedArtifactLocator.SUPPORT_BUNDLE_SYMBOLIC_NAME);
    }
    
    private OsgiClient newOsgiClient() {

        String hostname = getHostname();
        int launchpadPort = getPort();

        OsgiClientFactory factory = Activator.getDefault().getOsgiClientFactory();

        // TODO remove credential hardcoding
        return factory.createOsgiClient(new RepositoryInfo("admin", "admin", "http://" + hostname + ":" + launchpadPort
                + "/"));
    }
	
    IServer getOrCreateServer(IProgressMonitor monitor) {
		if (useExistingServer.getSelection()) {
			String key = existingServerCombo.getItem(existingServerCombo.getSelectionIndex());
			return serversMap.get(key);
		} else {
			IServerType serverType = ServerCore.findServerType("org.apache.sling.ide.launchpadServer");
			@SuppressWarnings("unused")
			IRuntime existingRuntime = null;//ServerCore.findRuntime("org.apache.sling.ide.launchpadRuntimeType");
			IRuntime[] existingRuntimes = ServerCore.getRuntimes();
			for (int i = 0; i < existingRuntimes.length; i++) {
				IRuntime aRuntime = existingRuntimes[i];
				if (aRuntime.getRuntimeType().getId().equals("org.apache.sling.ide.launchpadRuntimeType")) {
					existingRuntime = aRuntime;
				}
			}
			
            Version finalVersion = null;
			
			if (installToolingSupportBundle.getSelection()) {
                Version installedVersion;
                try {
                    installedVersion = getToolingSupportBundleVersion();
                } catch (OsgiClientException e) {
                    getWizard().reportError(e);
                    return null;
                }
                finalVersion = installedVersion;
                EmbeddedArtifactLocator artifactsLocator = Activator.getDefault().getArtifactsLocator();
                EmbeddedArtifact toolingSupportBundle = artifactsLocator.loadToolingSupportBundle();
                Version ourVersion = new Version(toolingSupportBundle.getVersion());

                if (installedVersion == null || ourVersion.compareTo(installedVersion) > 0) {
					// then auto-install it if possible
					try {

                        InputStream contents = null;
                        try {
                            contents = toolingSupportBundle.openInputStream();
                            newOsgiClient().installBundle(contents, toolingSupportBundle.getName());
                        } finally {
                            IOUtils.closeQuietly(contents);
                        }
                        finalVersion = ourVersion;
					} catch (IOException e) {
                        getWizard().reportError(e);
                        return null;
                    } catch (OsgiClientException e) {
                        getWizard().reportError(e);
                        return null;
                    }
				}
			}
			
			IRuntimeType serverRuntime = ServerCore.findRuntimeType("org.apache.sling.ide.launchpadRuntimeType");
			try {
                // TODO pass in username and password
                // TODO there should be a nicer API for creating this, and also a central place for defaults
                IRuntime runtime = serverRuntime.createRuntime(null, monitor);
                runtime = runtime.createWorkingCopy().save(true, monitor);
                IServerWorkingCopy wc = serverType.createServer(null, null, runtime, monitor);
				wc.setHost(getHostname());
				wc.setName(newServerName.getText() + " (external)");
				wc.setAttribute(ISlingLaunchpadServer.PROP_PORT, getPort());
				wc.setAttribute(ISlingLaunchpadServer.PROP_DEBUG_PORT, Integer.parseInt(newServerDebugPort.getText()));
                wc.setAttribute(ISlingLaunchpadServer.PROP_INSTALL_LOCALLY, installToolingSupportBundle.getSelection());
                wc.setAttribute("auto-publish-setting", ISlingLaunchpadServer.PUBLISH_STATE_RESOURCE_CHANGE);
                wc.setAttribute("auto-publish-time", 0);
                if (finalVersion != null) {
                    wc.setAttribute(String.format(ISlingLaunchpadServer.PROP_BUNDLE_VERSION_FORMAT,
                        EmbeddedArtifactLocator.SUPPORT_BUNDLE_SYMBOLIC_NAME), finalVersion.toString());
                }
				wc.setRuntime(runtime);
                return wc.save(true, monitor);
			} catch (CoreException e) {
                getWizard().reportError(e);
			}
			return null;
		}
	}

	private int getPort() {
		return Integer.parseInt(newServerPort.getText());
	}

	private String getHostname() {
		return newServerHostnameName.getText();
	}

}