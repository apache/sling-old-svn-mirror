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

package org.apache.sling.explorer.client.sling;



import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.HTML;

public class Sling {

    public static String SESSION_INFO_URL = "/system/sling/info.sessionInfo.json";
	private static String USER_ID = "userID";
	private static String WORKSPACE = "workspace";

	public void retrieveSessionInfo(HTML sessionInfoUI) {
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, SESSION_INFO_URL);

		try {
			builder.sendRequest(null, new JsonTreeRequestCallback(sessionInfoUI));
			GWT.log("send request end", null);

		} catch (RequestException e) {

			GWT.log("Couldn't retrieve User Information", e);

		}
	}

	/**
	 * This class is used as a request callback object for receiving the json
	 * structure provided by Sling. The json structure contains information on the current session
	 *
	 *
	 */
	private class JsonTreeRequestCallback implements RequestCallback {

		HTML sessionInfoUI;

		public JsonTreeRequestCallback(HTML sessionInfoUI) {
			this.sessionInfoUI = sessionInfoUI;
		}

		public void onError(Request request, Throwable exception) {
			GWT.log("Couldn't retrieve session information", null);
		}

		public void onResponseReceived(Request request, Response response) {
			if (200 == response.getStatusCode()) {
				SessionInfo sessionInfo = getSessionInfo(response);
				sessionInfoUI.setText("User : " +  sessionInfo.getUser() + " | " + sessionInfo.getWorkspace());
			} else {
				GWT.log("Couldn't retrieve JSON for session information (" + response.getStatusText()
						+ ")", null);
			}
		}

		private SessionInfo getSessionInfo(Response response) {

			try {
				// parse the response text into JSON
				JSONValue jsonValue = JSONParser.parse(response.getText());
				JSONObject jsonObject = jsonValue.isObject();

				if (jsonObject != null) {
					GWT.log("send request get value end", null);
					return new SessionInfo(jsonObject.get(USER_ID).toString(), jsonObject.get(WORKSPACE).toString());

                }
				throw new JSONException(
						"Invalid Json structure when retrieve the Sling nodes");
			} catch (JSONException e) {
				GWT.log("Could not parse JSON", e);
				throw new JSONException("Invalid Json structure when retrieve the Sling nodes");

			}
		}
	};
}
