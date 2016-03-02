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
import org.apache.sling.ide.eclipse.core.SetResolveSourcesCommand;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

public class DebugEditorSection extends ServerEditorSection {
    protected boolean _updating;
    protected PropertyChangeListener _listener;

    private Button resolveSourcesButton;
    private ISlingLaunchpadServer launchpadServer;
    private PropertyChangeListener serverListener;
    private Composite actionArea;

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);
        FormToolkit toolkit = getFormToolkit(parent.getDisplay());

        Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
                | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.FOCUS_TITLE);
        section.setText("Debug");
        section.setDescription("Resolving sources when connecting in debug mode ensure that you have up-to-date source attachments which reflect the "
                + "bundles running in the remote instance. However, initial resolve can be slow.");
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

        
        resolveSourcesButton = toolkit.createButton(composite, "Resolve sources when connecting", SWT.CHECK);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
        resolveSourcesButton.setLayoutData(data);
        
        actionArea = toolkit.createComposite(composite);
        RowLayout actionAreaLayout = new RowLayout();
        actionAreaLayout.center = true;
        actionArea.setLayout(actionAreaLayout);

        initialize();
    }

    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);

        serverListener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                if (ISlingLaunchpadServer.PROP_RESOLVE_SOURCES.equals(evt.getPropertyName())) {
            		resolveSourcesButton.setSelection((Boolean)evt.getNewValue());
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

        resolveSourcesButton.setSelection(config.resolveSourcesInDebugMode());

        SelectionListener listener = new SelectionAdapter() {
        	
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		execute(new SetResolveSourcesCommand(server, resolveSourcesButton.getSelection()));
        	}
		};

        resolveSourcesButton.addSelectionListener(listener);
    }

    @Override
    public void dispose() {
        if (server != null)
            server.removePropertyChangeListener(serverListener);

        super.dispose();
    }

}
