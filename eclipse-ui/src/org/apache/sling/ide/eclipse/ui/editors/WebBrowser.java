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
package org.apache.sling.ide.eclipse.ui.editors;

import org.apache.sling.ide.eclipse.ui.WhitelabelSupport;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class WebBrowser extends EditorPart {

	private Browser browser;
	private WebBrowserEditorInput in;

	@Override
	public void doSave(IProgressMonitor monitor) {
		
	}

	@Override
	public void doSaveAs() {
		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (input instanceof WebBrowserEditorInput) {
			in = (WebBrowserEditorInput) input;
			setPartName(in.getUrl());
		}
		setInput(input);
		setSite(site);
        setTitleImage(WhitelabelSupport.getProductIcon().createImage());
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);
		Button reload = new Button(container, SWT.NONE);
		reload.setText("Reload");
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 
				GridData.VERTICAL_ALIGN_FILL, false, false, 1, 2);
		reload.setLayoutData(gd);
		reload.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browser.setUrl(in.getUrl());
			}
		});
		Label urlLabel = new Label(container, SWT.NONE);
		urlLabel.setText("URL: "+in.getUrl());
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL, 
				GridData.VERTICAL_ALIGN_FILL, true, false, 1, 2);
		urlLabel.setLayoutData(gd);
        browser = new Browser(container, SWT.NONE); 
		browser.setUrl(in.getUrl());
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		browser.setLayoutData(gd);
	}

	@Override
	public void setFocus() {
		browser.setFocus();
	}

}
