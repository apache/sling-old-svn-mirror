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

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;

/**
 * Represents a NodeDefinition in JSON.
 *
 */
public class JSONNodeDefinition extends JSONItemDefinition {

	public JSONNodeDefinition(NodeDefinition childNodeDefinition) throws JSONException {
		super(childNodeDefinition);

		if (childNodeDefinition.allowsSameNameSiblings()){
			jsonObj.put("allowsSameNameSiblings", true);
		}
		NodeType defaultPrimaryType = childNodeDefinition.getDefaultPrimaryType();
		if (defaultPrimaryType!=null){
			String defaultPrimaryTypeName = defaultPrimaryType.getName();
			if (defaultPrimaryTypeName != null && !defaultPrimaryTypeName.equals("")){
				jsonObj.put("defaultPrimaryType", defaultPrimaryTypeName);
			}
		}

		NodeType[] primaryTypes = childNodeDefinition.getRequiredPrimaryTypes();
		List<String> primaryTypeNames = new ArrayList<String>();
		for (NodeType primaryType : primaryTypes) {
			String primaryTypeName = primaryType.getName();
			if (primaryTypeName != null) {
				primaryTypeNames.add(primaryTypeName);
			}
		}
		if (primaryTypeNames.size()>0 && !(primaryTypeNames.size()==1 && primaryTypeNames.get(0).equals("nt:base")) ){
			jsonObj.put("requiredPrimaryTypes", new JSONArray(primaryTypeNames));
		}
	}

}
