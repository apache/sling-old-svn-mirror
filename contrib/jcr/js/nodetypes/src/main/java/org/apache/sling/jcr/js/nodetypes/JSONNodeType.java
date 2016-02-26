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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Represents a NodeType in JSON.
 *
 */
public class JSONNodeType {

	private JSONObject jsonObj = new JSONObject();

	public JSONNodeType(NodeType nodeType) throws ValueFormatException, RepositoryException, JSONException {
		NodeDefinition[] declaredChildNodeDefinitions = nodeType.getDeclaredChildNodeDefinitions();
		if (declaredChildNodeDefinitions != null) {
			JSONArray jsonChildNodeDefArray = new JSONArray();
			for (NodeDefinition childNodeDefinition : nodeType.getDeclaredChildNodeDefinitions()) {
				String childNodeName = childNodeDefinition.getName();
				if (childNodeName != null) {
					JSONNodeDefinition jsonChildNodeDefinition = new JSONNodeDefinition(childNodeDefinition);
					jsonChildNodeDefArray.put(jsonChildNodeDefinition.getJSONObject());
				}
			}
			if (jsonChildNodeDefArray.length()>0){
				jsonObj.put("declaredChildNodeDefinitions",jsonChildNodeDefArray);
			}
		}

		PropertyDefinition[] declaredPropertyDefinitions = nodeType.getDeclaredPropertyDefinitions();
		if (declaredPropertyDefinitions != null) {
			JSONArray jsonPropDefArray = new JSONArray();
			for (PropertyDefinition propertyDefinition : declaredPropertyDefinitions) {
				JSONPropertyDefinition jsonPropertyDefinition = new JSONPropertyDefinition(propertyDefinition);
				jsonPropDefArray.put(jsonPropertyDefinition.getJSONObject());
			}
			if (jsonPropDefArray.length()>0){
				jsonObj.put("declaredPropertyDefinitions",jsonPropDefArray);
			}
		}

		NodeType[] superTypes = nodeType.getDeclaredSupertypes();
		List<String> superTypeNames = new ArrayList<String>();
		for (NodeType superType : superTypes) {
			superTypeNames.add(superType.getName());
		}
		if (superTypeNames.size()>0 && !("nt:base".equals(superTypeNames.get(0)) && superTypeNames.size()==1)){
			jsonObj.put("declaredSupertypes", new JSONArray(superTypeNames));
		}
		if (nodeType.isMixin()){
			jsonObj.put("mixin", true);
		}
		if (nodeType.hasOrderableChildNodes()){
			jsonObj.put("orderableChildNodes", true);
		}
		String thePrimaryItemName = nodeType.getPrimaryItemName();
		if (thePrimaryItemName != null && !thePrimaryItemName.equals("")){
			jsonObj.put("primaryItemName", nodeType.getPrimaryItemName());
		}
	}
	
	JSONObject getJson(){
		return jsonObj;
	}

}
