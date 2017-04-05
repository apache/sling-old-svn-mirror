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
package org.apache.sling.ide.eclipse.ui.wizards;

import static org.apache.sling.ide.eclipse.ui.internal.SlingLaunchpadCombo.ValidationFlag.SKIP_SERVER_STARTED;


import org.apache.sling.ide.eclipse.core.ISlingLaunchpadConfiguration;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.apache.sling.ide.eclipse.core.SlingLaunchpadConfigurationDefaults;
import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.eclipse.ui.internal.SlingLaunchpadCombo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

public class SetupServerWizardPage extends WizardPage {
	
    private static final String RUNTIME_TYPE_LAUNCHPAD = "org.apache.sling.ide.launchpadRuntimeType";

    private static final String SERVER_TYPE_LAUNCHPAD = "org.apache.sling.ide.launchpadServer";

    private static final int HORIZONTAL_INDENT = 10;

    private Button useExistingServer;
    private SlingLaunchpadCombo existingServerCombo;
	private Button setupNewServer;
	private Text newServerName;
	private Text newServerHostnameName;
	private Text newServerPort;
    private Text newServerUsername;
    private Text newServerPassword;
	private Text newServerDebugPort;
	
    private IServer server;

    private Button startExistingServerButton;

    private Button skipServerConfiguration;

    private ISlingLaunchpadConfiguration config;

    public SetupServerWizardPage(AbstractNewSlingApplicationWizard parent) {
		super("chooseArchetypePage");
        setTitle("Select or Create Server");
        setDescription("This step defines which server to use with the new project.");
		setImageDescriptor(parent.getLogo());
		config = parent.getDefaultConfig();
	}

    @Override
    public AbstractNewSlingApplicationWizard getWizard() {
        return (AbstractNewSlingApplicationWizard) super.getWizard();
    }

	public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.marginBottom = 10;

        GridDataFactory singleRowGridDataFactory = GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.CENTER)
                .span(layout.numColumns, 1);

		useExistingServer = new Button(container, SWT.RADIO);
	    useExistingServer.setText("Add to existing server");
        singleRowGridDataFactory.applyTo(useExistingServer);

	    Label existingServerLabel = new Label(container, SWT.NONE);
	    GridData locationLabelData = new GridData();
        locationLabelData.horizontalIndent = HORIZONTAL_INDENT;
	    existingServerLabel.setLayoutData(locationLabelData);
	    existingServerLabel.setText("Location:");
	    existingServerLabel.setEnabled(true);

        existingServerCombo = new SlingLaunchpadCombo(container, null);
        existingServerCombo.getWidget().addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent e) {
	    	  dialogChanged();
	      }
	    });
        existingServerCombo.refreshRepositoryList(new NullProgressMonitor());
        existingServerCombo.getWidget().setEnabled(true);

        {
            startExistingServerButton = new Button(container, SWT.CHECK);
            GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
            gd.horizontalIndent = HORIZONTAL_INDENT;
            startExistingServerButton.setLayoutData(gd);
            startExistingServerButton.setText("Start server after project creation (if server not yet started).");
            startExistingServerButton.setSelection(true);
        }

        skipServerConfiguration = new Button(container, SWT.RADIO);
        skipServerConfiguration.setText("Don't deploy on a server");
        singleRowGridDataFactory.applyTo(skipServerConfiguration);

        setupNewServer = new Button(container, SWT.RADIO);
        setupNewServer.setText("Setup new server");
        singleRowGridDataFactory.applyTo(setupNewServer);
	    
        newLabel(container, "Server name:");
        newServerName = newText(container);
	    
        newLabel(container, "Host name:");
        newServerHostnameName = newText(container);
        newServerHostnameName.setText("localhost");
	    
        newLabel(container, "Port:");
        newServerPort = newText(container);
        newServerPort.setText(Integer.toString(config.getPort()));
        
        newLabel(container, "Username:");
        newServerUsername = newText(container);
        newServerUsername.setText(config.getUsername());

        newLabel(container, "Password:");
        newServerPassword = newText(container);
        newServerPassword.setText(config.getPassword());
	    
        newLabel(container, "Debug Port:");
        newServerDebugPort = newText(container);
        newServerDebugPort.setText(Integer.toString(config.getDebugPort()));
	    
	    SelectionAdapter radioListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                updateEnablements();
				dialogChanged();
			}
        };
		useExistingServer.addSelectionListener(radioListener);
		setupNewServer.addSelectionListener(radioListener);
	    
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

        useExistingServer.setSelection(existingServerCombo.hasServers());
        existingServerCombo.getWidget().setEnabled(existingServerCombo.hasServers());
        startExistingServerButton.setEnabled(existingServerCombo.hasServers());
        setupNewServer.setSelection(!existingServerCombo.hasServers());

        updateEnablements();

        setPageComplete(false);
		
		setControl(container);
		
		// allow the selection to proceed in case we have a preselected server
        if (useExistingServer.getSelection()) {
            if (existingServerCombo.getErrorMessage(SKIP_SERVER_STARTED) == null) {
                updateStatus(null);
            }
        }
	}

    private Label newLabel(Composite container, String text) {

        Label label = new Label(container, SWT.NONE);
        GridData newServerPortLabelData = new GridData();
        newServerPortLabelData.horizontalIndent = HORIZONTAL_INDENT;
        label.setLayoutData(newServerPortLabelData);
        label.setEnabled(true);
        label.setText(text);

        return label;
    }

    private Text newText(Composite container) {

        Text text = new Text(container, SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        return text;
    }

	private void dialogChanged() {

        // called too early
        if (getControl() == null) {
            return;
        }

		if (useExistingServer.getSelection()) {
            if (existingServerCombo.getErrorMessage(SKIP_SERVER_STARTED) != null) {
                updateStatus(existingServerCombo.getErrorMessage());
				return;
			}
		} else if (setupNewServer.getSelection()) {
			if (newServerName.getText().length()==0 ||
					getHostname().length()==0 ||
					newServerPort.getText().length()==0 ||
					newServerDebugPort.getText().length()==0 ||
					newServerUsername.getText().length() == 0 ||
					newServerPassword.getText().length() == 0) {
				updateStatus("Enter values for new server");
				return;
			}
		}
		updateStatus(null);
	}

    private void updateEnablements() {

        existingServerCombo.getWidget().setEnabled(useExistingServer.getSelection());
        startExistingServerButton.setEnabled(useExistingServer.getSelection());
        newServerName.setEnabled(setupNewServer.getSelection());
        newServerHostnameName.setEnabled(setupNewServer.getSelection());
        newServerPort.setEnabled(setupNewServer.getSelection());
        newServerDebugPort.setEnabled(setupNewServer.getSelection());
        newServerUsername.setEnabled(setupNewServer.getSelection());
        newServerPassword.setEnabled(setupNewServer.getSelection());
    }

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
    
    public boolean getStartServer() {
        if (!useExistingServer.getSelection()) {
            return true; // new servers are automatically started
        }
        return startExistingServerButton.getSelection();
    }
	
    /**
     * Gets or creates a <tt>IServer</tt> instance to deploy projects on
     * 
     * @param monitor
     * @return the server instance, possibly null if the user requested to skip deployment
     * @throws CoreException
     */
    public IServer getOrCreateServer(IProgressMonitor monitor) throws CoreException {

        if (skipServerConfiguration.getSelection()) {
            return null;
        }

        if (server != null) {
            return server;
        }

		if (useExistingServer.getSelection()) {
            return existingServerCombo.getServer();
		} else {
		    
			IServerType serverType = ServerCore.findServerType(SERVER_TYPE_LAUNCHPAD);
			IRuntime slingRuntime = getOrCreateSlingRuntime(monitor);
			
			try {
                // TODO there should be a nicer API for creating this
                IServerWorkingCopy wc = serverType.createServer(null, null, slingRuntime, monitor);
				wc.setHost(getHostname());
                wc.setName(newServerName.getText());
				wc.setAttribute(ISlingLaunchpadServer.PROP_PORT, getPort());
				wc.setAttribute(ISlingLaunchpadServer.PROP_DEBUG_PORT, Integer.parseInt(newServerDebugPort.getText()));
                wc.setAttribute(ISlingLaunchpadServer.PROP_USERNAME, newServerUsername.getText());
                wc.setAttribute(ISlingLaunchpadServer.PROP_PASSWORD, newServerPassword.getText());
                
                SlingLaunchpadConfigurationDefaults.applyDefaultValues(wc);
                
				wc.setRuntime(slingRuntime);
                server = wc.save(true, monitor);
                return server;
			} catch (CoreException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Failed creating the new server instance", e));

			}
		}
	}

    private IRuntime getOrCreateSlingRuntime(IProgressMonitor monitor) throws CoreException {
        
        for ( IRuntime runtime : ServerCore.getRuntimes()) {
            if ( runtime.getRuntimeType().getId().equals(RUNTIME_TYPE_LAUNCHPAD)) {
                return runtime;
            }
        }
        
        IRuntimeType serverRuntime = ServerCore.findRuntimeType(RUNTIME_TYPE_LAUNCHPAD);
        return serverRuntime.createRuntime(null, monitor).createWorkingCopy().save(true, monitor);
    }

	private int getPort() {
		return Integer.parseInt(newServerPort.getText());
	}

	private String getHostname() {
		return newServerHostnameName.getText();
	}

}