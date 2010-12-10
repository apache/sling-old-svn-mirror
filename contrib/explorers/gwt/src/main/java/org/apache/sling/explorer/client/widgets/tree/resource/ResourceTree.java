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
package org.apache.sling.explorer.client.widgets.tree.resource;

import org.apache.sling.explorer.client.ExplorerConstants;
import org.apache.sling.explorer.client.widgets.grid.resource.ResourceGrids;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class ResourceTree extends Tree {

	private TreeItem root;
	private ResourceGrids properties;
	private ExplorerConstants constants;

	public ResourceTree(ResourceGrids properties) {
		this();
		this.properties = properties;
		// Create the constants
	    constants = (ExplorerConstants) GWT.create(ExplorerConstants.class);
	}

	public ResourceTree() {
		super();

		// Add an open handler to have a lazy loading treeview
		addOpenHandler(new OpenHandler<TreeItem>() {
		      public void onOpen(OpenEvent<TreeItem> event) {
		          TreeItem item = event.getTarget();
		          if (item.getState() && item.getChildCount() == 1) {

						Node node = (Node) item.getUserObject();
						loadChildren(item, node.getId() + ExplorerConstants.JSON_TREE_REQUEST_EXTENSION);

						// Remove the temporary item when we finish loading
						item.getChild(0).remove();

					}


		        }
		      });

		// Add a selection handler to refresh the property & resource grids
		addSelectionHandler(new SelectionHandler<TreeItem> (){

			public void onSelection(SelectionEvent<TreeItem> event) {
				GWT.log("Selected item : " + event.getSelectedItem().getText(), null);
				Node node = (Node) event.getSelectedItem().getUserObject();
				if (properties != null)
					properties.populate(node.id);

			}


		});
	}

	public void populate() {
		root = new TreeItem(constants.rootItemDescription());
//		try {
//
//            SessionInfo info = (SessionInfo)   this.getClass().getClassLoader().loadClass("org.apache.sling.explorer.client.sling.SessionInfo").newInstance();
//
//            root = new TreeItem(info.getUser());
//        }
//        catch(Exception ex) {
//            return;
//        }
		addItem(root);
		this.loadChildren(root, URL.encode(ExplorerConstants.CONTENT_ROOT + ExplorerConstants.JSON_TREE_REQUEST_EXTENSION));
		if (properties != null) {
            properties.populate(ExplorerConstants.CONTENT_ROOT );
		}
	}

	private void loadChildren(final TreeItem treeItem, final String url) {

		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);

		try {
			builder.sendRequest(null, new JsonTreeRequestCallback(treeItem));
		} catch (RequestException e) {
			// TODO : add message box for the end-user
			GWT.log("ResourceTree - Couldn't retrieve JSON for url : " + url + ExplorerConstants.JSON_TREE_REQUEST_EXTENSION, e);
		}

	}

	/**
	 * This class is used as a request callback object for receiving the json
	 * structure provided by Sling. The json structure contains the node
	 * children that we have to display in the tree
	 *
	 *
	 */
	private class JsonTreeRequestCallback implements RequestCallback {
		private TreeItem treeItem;

		public JsonTreeRequestCallback(TreeItem treeItem) {
			this.treeItem = treeItem;
		}

		public void onError(Request request, Throwable exception) {
			GWT.log("ResourceTree  - on error for request : " + request.toString(), null);
		}

		public void onResponseReceived(Request request, Response response) {
			if (200 == response.getStatusCode()) {
				addTreeItems(response);
				if (treeItem.getText().equals(constants.rootItemDescription()))
					treeItem.setState(true, true);
			} else {
				GWT.log("ResourceTree - Couldn't retrieve JSON for request : " + request.toString(), null);
			}
		}

		private void addTreeItems(Response response) {
			GWT.log(response.getText(), null);
			try {
				// parse the response text into JSON
				JSONValue jsonValue = JSONParser.parse(response.getText());
				JSONArray jsonArray = jsonValue.isArray();

				if (jsonArray != null) {
					for (int index = 0; index < jsonArray.size(); index++) {
						addTreeItem(((JSONObject) jsonArray.get(index)), index);
					}
				} else {
					throw new JSONException(
							"Invalid Json structure when retrieve the Sling nodes");
				}
			} catch (JSONException e) {
				e.printStackTrace();
				GWT.log("ResourceTree - Could not parse JSON", e);
			}
		}

		private void addTreeItem(JSONObject jsonObject, int index) {

			Node node = new Node(jsonObject.get("id").isString().stringValue(),
					jsonObject.get("leaf").isBoolean().booleanValue(),
					jsonObject.get("text").isString().stringValue());
			if (node.getText() != null) {
				TreeItem item = new TreeItem();
				item.setText(node.getText());
				item.setUserObject(node);
				if (!node.isLeaf())
					item.addItem(""); // Temporarily add an item so we can expand this node

				treeItem.addItem(item);

			}
		}

	};

	/**
	 * User object used for the treeview items. It contains information on the
	 * associated node
	 *
	 */
	private class Node {

		private String id;
		private String text;
		private boolean leaf;

		public Node(String id, boolean leaf, String text) {
			super();
			this.id = id;
			this.leaf = leaf;
			this.text = text;
		}

		public String getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		public boolean isLeaf() {
			return leaf;
		}
	}
}
