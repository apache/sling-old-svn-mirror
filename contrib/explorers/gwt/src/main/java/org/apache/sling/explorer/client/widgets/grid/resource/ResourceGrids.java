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
package org.apache.sling.explorer.client.widgets.grid.resource;


import org.apache.sling.explorer.client.ExplorerConstants;
import org.apache.sling.explorer.client.widgets.grid.ExplorerGrid;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.FlexTable;

/**
 *
 * A ResourcesGrids display information on a resources
 * It is composed of 2 different Grids.
 * One for the resource properties & another one for the resource children.
 *
 */
public class ResourceGrids {


	private ExplorerGrid propertyGrid = new ExplorerGrid();
	private ExplorerGrid resourceGrid = new ExplorerGrid();

	public ResourceGrids() {
		super();
		// Add the Header to the property grid
		Object[] cols = new Object[4];

		// TODO : Change the hardcoded labels
		cols[0] = "Name";
		cols[1] = "Type";
		cols[2] = "Value";
		cols[3] = "Multi";

		propertyGrid.AddHeader(cols);

		//  Add the Header to the Subnode grid
		// TODO : Change the hardcoded labels
		cols = new Object[2];
		cols[0] = "Name";
		cols[1] = "Type";
		resourceGrid.AddHeader(cols);
	}

	private void removeAllRowsInGrids() {

		int rowCount = propertyGrid.getRowCount();
		// Loop from i=1 because we don't remove the grid label
		for (int i=rowCount-1; i>0; i-- )
		{
			propertyGrid.removeRow(i);
		}

	     rowCount = resourceGrid.getRowCount();
		// Loop from i=1 because we don't remove the grid label
	     for (int i=rowCount-1; i>0; i-- )
		{
			resourceGrid.removeRow(i);
		}

	}

	public void populate(String url) {
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url + ExplorerConstants.JSON_CHILDREN_REQUEST_EXTENSION);

		try {
			builder.sendRequest(null, new JsonGridCallback(this));
		} catch (RequestException e) {
			// TODO : add message box for the end-user
			GWT.log("ResourceGrids - Couldn't retrieve JSON for url : " + url + ExplorerConstants.JSON_CHILDREN_REQUEST_EXTENSION, e);
		}
	}

	public FlexTable getPropertyGrid() {
		return propertyGrid;
	}

	public FlexTable getResourceChildrenGrid() {
		return resourceGrid;
	}

	private class JsonGridCallback implements RequestCallback {
		private ResourceGrids grid;

		public JsonGridCallback(ResourceGrids grid) {
			this.grid = grid;
		}

		public void onError(Request request, Throwable exception) {
			GWT.log("ResourceGrids  - on error for request : " + request.toString(), null);
		}

		public void onResponseReceived(Request request, Response response) {
			if (200 == response.getStatusCode()) {
				grid.removeAllRowsInGrids();
				addProperties(response);

			} else {
				GWT.log("ResourceGrids - Couldn't retrieve JSON for request - status code  :"
						+ response.getStatusCode() + " -  "
						+ response.getText(), null);
			}
		}

		private void addProperties(Response response) {
			GWT.log(response.getText(), null);
			try {
				// parse the response text into JSON
				JSONValue jsonValue = JSONParser.parse(response.getText());
				JSONArray jsonArray = jsonValue.isArray();

				if (jsonArray != null) {
					for (int index = 0; index < jsonArray.size(); index++) {
						addProperty(((JSONObject) jsonArray.get(index)), index);
					}
				} else {
					throw new JSONException(
							"Invalid Json structure when retrieve the Sling nodes");
				}
			} catch (JSONException e) {
				e.printStackTrace();
				GWT.log("ResourceGrids - Could not parse JSON", e);
			}
		}

		private void addProperty(JSONObject jsonObject, int index) {

			Item item = new Item(jsonObject.get("itemType").isString().stringValue(),
					jsonObject.get("multi").isBoolean().booleanValue(),
					jsonObject.get("name").isString().stringValue(),
					jsonObject.get("type").isString().stringValue(),
					jsonObject.get("value").isString().stringValue());

			if (item.getItemType().equals(ExplorerConstants.PROPERTY))
				addToPropertyGrid(propertyGrid, item);
			else
			   addToChildrenResourceGrid(resourceGrid, item);

		}

		private void addToPropertyGrid( ExplorerGrid grid, Item item) {
			Object[] cols = new Object[4];
			cols[0] = item.getName();
			cols[1] = item.getType();
			cols[2] = item.getValue();
			cols[3] = item.isMutli().toString();

			grid.addRow(grid.getRowCount(), cols);
		}

		private void addToChildrenResourceGrid( ExplorerGrid grid, Item item) {

			Object[] cols = new Object[2];
			cols[0] = item.getName();
			cols[1] = item.getType();

			grid.addRow(grid.getRowCount(), cols);
		}

	};

	/**
	 * User object used for the treeview items (resource children or resource properties).
	 *
	 */
	private class Item {

		private String name;
		private String type;
		private String value;
		private Boolean mutli;
		private String itemType;

		public Item(String itemType, Boolean mutli, String name, String type,
				String value) {
			super();
			this.itemType = itemType;
			this.mutli = mutli;
			this.name = name;
			this.type = type;
			this.value = value;
		}

		public String getName() {
			return name;
		}
		public String getType() {
			return type;
		}
		public String getValue() {
			return value;
		}
		public Boolean isMutli() {
			return mutli;
		}
		public String getItemType() {
			return itemType;
		}
	}
}
