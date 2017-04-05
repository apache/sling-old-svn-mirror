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
package org.apache.sling.ide.eclipse.ui.views;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrProperty;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DateTimeEditor extends Dialog {

    private final JcrProperty property;
    private TableViewer viewer;
    private Label result;
    private DateTime calendar;
    private DateTime time;
    private String dateAsString;
    private Calendar c;

    protected DateTimeEditor(Shell parentShell, JcrProperty property) {
        super(parentShell);
        this.property = property;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Modify date/time property");
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        
        GridData parentLayoutData = new GridData(GridData.FILL_BOTH);
        parentLayoutData.widthHint = 280;
        parentLayoutData.heightHint = 280;
        composite.setLayoutData(parentLayoutData);
        GridLayout parentLayout = (GridLayout) composite.getLayout();
        parentLayout.numColumns = 2;
        
        Label label = new Label(composite, SWT.WRAP);
        label.setText("Modify property "+property.getName()+":");
        GridData data = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_CENTER);
        data.horizontalSpan = 2;
        data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
        label.setLayoutData(data);
        label.setFont(parent.getFont());
        
        Label hline = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
        layoutData.horizontalSpan = 2;
        hline.setLayoutData(layoutData);
        
        Label dateLabel = new Label(composite, SWT.WRAP);
        dateLabel.setText("Date:");
        layoutData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING);
        layoutData.widthHint = 80;
        dateLabel.setLayoutData(layoutData);
        dateLabel.setFont(parent.getFont());

        calendar = new DateTime(composite, SWT.CALENDAR);
        layoutData = new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
        layoutData.horizontalSpan = 1;
        calendar.setLayoutData(layoutData);
        calendar.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateSelection();
            }
        });
        
        Label timeLabel = new Label(composite, SWT.WRAP);
        timeLabel.setText("Time:");
        layoutData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING);
        layoutData.widthHint = 80;
        timeLabel.setLayoutData(layoutData);
        timeLabel.setFont(parent.getFont());

        time = new DateTime(composite, SWT.TIME);
        layoutData = new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
        layoutData.horizontalSpan = 1;
        time.setLayoutData(layoutData);
        time.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateSelection();
            }
        });
        
        hline = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        layoutData.horizontalSpan = 2;
        hline.setLayoutData(layoutData);

        result = new Label(composite, SWT.WRAP);
        result.setText("Foo");
        data = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_CENTER);
        data.horizontalSpan = 2;
        result.setLayoutData(data);
        result.setFont(parent.getFont());

        // initialize value
        dateAsString = property.getValueAsString();
        c = DateTimeSupport.parseAsCalendar(dateAsString);
        calendar.setDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        time.setTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
        
        updateSelection();
        return composite;
    }
    
    protected void updateSelection() {
        int day = calendar.getDay();
        int month = calendar.getMonth();
        int year = calendar.getYear();
        int hours = time.getHours();
        int minutes = time.getMinutes();
        int seconds = time.getSeconds();
        c = new GregorianCalendar(year, month, day, hours, minutes, seconds);
        dateAsString = DateTimeSupport.print(c);
        result.setText(dateAsString);
    }

    @Override
    protected void okPressed() {
        super.okPressed();
    }

    public Date getDate() {
        return c.getTime();
    }
}
