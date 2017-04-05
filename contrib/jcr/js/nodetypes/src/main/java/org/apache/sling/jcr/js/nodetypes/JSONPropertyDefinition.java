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

import java.util.Arrays;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;

/**
 * Represents a PropertyDefinition in JSON.
 *
 */
public class JSONPropertyDefinition extends JSONItemDefinition {

	public JSONPropertyDefinition(PropertyDefinition propertyDefinition) throws ValueFormatException, RepositoryException, JSONException {
		super(propertyDefinition);

		Value[] defaultValues = propertyDefinition.getDefaultValues();
		JSONArray defaultValueArray = new JSONArray();
		if (defaultValues != null) {
			if (defaultValues !=null){
				for (int i=0; i<defaultValues.length; i++){
					Value defaultValue = defaultValues[i];
					JSONValue jsonValue = new JSONValue(defaultValue, i, propertyDefinition);
					defaultValueArray.put(jsonValue.getJSONObject());
				}
			}
		}

		if (defaultValueArray.length()>0){
			jsonObj.put("defaultValues", defaultValueArray);
		}
		String theRequiredType = PropertyType.nameFromValue(propertyDefinition.getRequiredType());
		if (theRequiredType!=null && !theRequiredType.equals("") && !("String".equals(theRequiredType))){
			jsonObj.put("requiredType", theRequiredType);
		}
		if (propertyDefinition.getValueConstraints()!=null){
			List<String> theValueConstraints = Arrays.asList(propertyDefinition.getValueConstraints());
			if (theValueConstraints != null && theValueConstraints.size()>0){
				jsonObj.put("valueConstraints", theValueConstraints);
			}
		}
		if (propertyDefinition.isMultiple()){
			jsonObj.put("multiple", true);
		}
	}
}
