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
package org.apache.sling.jcr.js.nodetypes;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Represents an ItemDefinition in JSON.
 */
public class JSONItemDefinition {

	protected JSONObject jsonObj = new JSONObject();
	
	public JSONItemDefinition(ItemDefinition itemDefinition) throws JSONException {
		
		jsonObj.put("name", itemDefinition.getName());
		
		if (itemDefinition.isAutoCreated()){
			jsonObj.put("autoCreated", true);
		}
		if (itemDefinition.isMandatory()){
			jsonObj.put("mandatory", true);
		}
		if (itemDefinition.isProtected()){
			jsonObj.put("protected", true);
		}
		boolean onParentVersionIsUnset = itemDefinition.getOnParentVersion() == 0;
		int onParentVersion = onParentVersionIsUnset ? OnParentVersionAction.COPY : itemDefinition.getOnParentVersion();
		String onParentVersionAction = OnParentVersionAction.nameFromValue(onParentVersion);
		if (!"COPY".equals(onParentVersionAction)){
			jsonObj.put("onParentVersion", onParentVersionAction);
		}
	}
	
	JSONObject getJSONObject(){
		return jsonObj;
	}
	
}
