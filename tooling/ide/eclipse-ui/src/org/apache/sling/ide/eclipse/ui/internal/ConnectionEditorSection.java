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

import org.apache.sling.ide.eclipse.core.ISlingLaunchpadConfiguration;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.apache.sling.ide.eclipse.core.SetServerContextPathCommand;
import org.apache.sling.ide.eclipse.core.SetServerDebugPortCommand;
import org.apache.sling.ide.eclipse.core.SetServerPasswordCommand;
import org.apache.sling.ide.eclipse.core.SetServerPortCommand;
import org.apache.sling.ide.eclipse.core.SetServerUsernameCommand;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

public class ConnectionEditorSection extends ServerEditorSection {
    protected boolean _updating;
    protected PropertyChangeListener _listener;

    private Text portText;
    private Text debugPortText;
    private Text contextPathText;
    private Text usernameText;
    private Text passwordText;
    private ISlingLaunchpadServer launchpadServer;
    private PropertyChangeListener serverListener;
    private boolean updating = false;

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);
        FormToolkit toolkit = getFormToolkit(parent.getDisplay());

        Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
                | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.FOCUS_TITLE);
        section.setText("Connection");
        section.setDescription("Connection details for this server");
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

        createLabel(toolkit, composite, "Port");
        portText = createText(toolkit, composite, SWT.SINGLE);

        createLabel(toolkit, composite, "Debug Port");
        debugPortText = createText(toolkit, composite, SWT.SINGLE);

        createLabel(toolkit, composite, "Context path");
        contextPathText = createText(toolkit, composite, SWT.SINGLE);

        // TODO wrong parent
        Label separator = toolkit.createSeparator(parent, SWT.HORIZONTAL);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        separator.setLayoutData(data);

        createLabel(toolkit, composite, "Username");
        usernameText = createText(toolkit, composite, SWT.SINGLE);

        createLabel(toolkit, composite, "Password");
        passwordText = createText(toolkit, composite, SWT.PASSWORD);

        initialize();
    }

    private void createLabel(FormToolkit toolkit, Composite composite, String label) {
        Label portLabel = toolkit.createLabel(composite, label);
        portLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalIndent = 20;
        data.widthHint = 20;
        portLabel.setLayoutData(data);
    }

    private Text createText(FormToolkit toolkit, Composite composite, int flags) {
        Text port = toolkit.createText(composite, "", flags);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = 30;
        port.setLayoutData(data);

        return port;
    }

    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);

        serverListener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
            	if (updating) {
            		return;
            	}
            	updating = true;
            	try{
            	    switch ( evt.getPropertyName()) {
            	        case ISlingLaunchpadServer.PROP_PORT:
            	            portText.setText(((Integer) evt.getNewValue()).toString());
            	            break;
            	            
            	        case ISlingLaunchpadServer.PROP_DEBUG_PORT:
            	            debugPortText.setText(((Integer) evt.getNewValue()).toString());
            	            break;
            	            
            	        case ISlingLaunchpadServer.PROP_CONTEXT_PATH:
            	            contextPathText.setText((String) evt.getNewValue());
            	            break;
            	            
            	        case ISlingLaunchpadServer.PROP_USERNAME:
            	            usernameText.setText((String) evt.getNewValue());
            	            break;
            	            
            	        case ISlingLaunchpadServer.PROP_PASSWORD:
            	            passwordText.setText((String) evt.getNewValue());
            	            break;
            	            
            	    }
            	} finally {
            		updating = false;
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
    }

    private void initialize() {

        final ISlingLaunchpadConfiguration config = launchpadServer.getConfiguration();

        portText.setText(String.valueOf(config.getPort()));
        debugPortText.setText(String.valueOf(config.getDebugPort()));
        contextPathText.setText(config.getContextPath());

        usernameText.setText(config.getUsername());
        passwordText.setText(config.getPassword());

        ModifyListener listener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
            	if (updating) {
            		return;
            	}
            	updating = true;
            	try{
	            	if (e.getSource() == portText) {
	                    try {
	                        int port = Integer.parseInt(portText.getText());
	                        execute(new SetServerPortCommand(server, port));
	                    } catch (NumberFormatException ex) {
	                        // shucks
	                    }
	                } else if (e.getSource() == debugPortText) {
	                    try {
	                        int debugPort = Integer.parseInt(debugPortText.getText());
	                        execute(new SetServerDebugPortCommand(server, debugPort));
	                    } catch (NumberFormatException ex) {
	                        // shucks
	                    	ex.printStackTrace();
	                    }
	                } else if (e.getSource() == contextPathText) {
	                    execute(new SetServerContextPathCommand(server, contextPathText.getText()));
	                } else if (e.getSource() == usernameText) {
	                    execute(new SetServerUsernameCommand(server, usernameText.getText()));
	                } else if (e.getSource() == passwordText) {
	                    execute(new SetServerPasswordCommand(server, passwordText.getText()));
	                }
            	} finally {
            		updating = false;
            	}
            }
        };

        for (Text text : new Text[] { portText, debugPortText, contextPathText, usernameText, passwordText }) {
            text.addModifyListener(listener);
        }

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
