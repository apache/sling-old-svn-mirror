/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.explorer.client.widgets.grid;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * Reusable Grid that can be used in the Sling Explorer
 *
 * Right now, the Grid is based on the GWT FlexTable widget
 *
 *
 */
public class ExplorerGrid extends FlexTable {

	public ExplorerGrid() {
		super();
		setStyleName("application-FlexTable");
		getRowFormatter().addStyleName(0, "application-FlexTable-ColumnLabel");

	}

	public void addRow(Integer rowIndex, Object[] cellObjects) {

		HTMLTable.RowFormatter rf = getRowFormatter();
		for (int cell = 0; cell < cellObjects.length; cell++) {
			Widget widget = createCellWidget(cellObjects[cell]);
			setWidget(rowIndex, cell, widget);

			if (cell==0)
			   getCellFormatter().addStyleName(rowIndex, cell,"application-FlexTable-Cell-first");
			else
			   getCellFormatter().addStyleName(rowIndex, cell,"application-FlexTable-Cell");


			if ((rowIndex % 2) != 0) {
				rf.addStyleName(rowIndex, "application-FlexTable-OddRow");
			} else {
				rf.addStyleName(rowIndex, "application-FlexTable-EvenRow");
			}
		}
	}

	public void AddHeader(Object[] cellObjects) {
		for (int cell = 0; cell < cellObjects.length; cell++) {
			Widget widget = createCellWidget(cellObjects[cell]);
			setWidget(0, cell, widget);
			getCellFormatter().addStyleName(0, cell,"application-FlexTable-ColumnLabelCell");
		}
	}

	private Widget createCellWidget(Object cellObject) {
		Widget widget = null;

		if (cellObject instanceof Widget)
			widget = (Widget) cellObject;
		else
			widget = new Label(cellObject.toString());

		return widget;
	}
}
