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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Represents a javax.jcr.Value in JSON.
 *
 */
public class JSONValue {

	transient JSONObject jsonObj = new JSONObject();
	
    public JSONValue(Value aValue, int index, PropertyDefinition propertyDef) throws ValueFormatException, RepositoryException, IllegalStateException, JSONException{
    	switch (aValue.getType()) {
		case PropertyType.STRING:
			jsonObj.put("string", aValue.getString());
			break;
		case PropertyType.DATE:
	    	String date = aValue.getDate() == null ? "" : ISO8601.format(aValue.getDate());
			jsonObj.put("date", date);
			break;
		case PropertyType.BINARY:
			String binary = getBinaryDownloadURLFromPropertyDef(index, propertyDef);
			jsonObj.put("binary", binary);
			break;
		case PropertyType.DOUBLE:
			jsonObj.put("double", aValue.getDouble());
			break;
		case PropertyType.LONG:
			jsonObj.put("long", aValue.getLong());
			break;
		case PropertyType.BOOLEAN:
			jsonObj.put("boolean", aValue.getBoolean());
			break;
		case PropertyType.NAME:
			jsonObj.put("name",  aValue.getString());
			break;
		case PropertyType.PATH:
			jsonObj.put("path",  aValue.getString());
			break;
		case PropertyType.REFERENCE:
			jsonObj.put("reference",  aValue.getString());
			break;
		case PropertyType.UNDEFINED:
			jsonObj.put("undefined",  aValue.getString());
			break;
/// The following property types are from JSR-283 / JCR 2.0
		case PropertyType.WEAKREFERENCE:
			jsonObj.put("weakReference",  aValue.getString());
			break;
		case PropertyType.URI:
			jsonObj.put("uri",  aValue.getString());
			break;
		case PropertyType.DECIMAL:
			String decimal = aValue.getDecimal() == null ? "" : aValue.getDecimal().toString();
			jsonObj.put("decimal", decimal);
			break;

		default:
			break;
		}
    	String type = PropertyType.nameFromValue(aValue.getType());
		jsonObj.put("type", type);
	}

	private String getBinaryDownloadURLFromPropertyDef(int index, PropertyDefinition propertyDef) {
		String nodeTypeName = propertyDef.getDeclaringNodeType().getName();
		String propertyName = propertyDef.getName();
		String propertyType = PropertyType.nameFromValue(propertyDef.getRequiredType());
		boolean isAutoCreated = propertyDef.isAutoCreated();
		boolean isMandatory = propertyDef.isMandatory();
		boolean isProtected = propertyDef.isProtected();
		boolean isMultiple = propertyDef.isMultiple();
		String onParentVersionAction = OnParentVersionAction.nameFromValue(propertyDef.getOnParentVersion());
		return String.format("/%s/%s/%s/%s/%s/%s/%s/%s/%s.default_binary_value.bin", nodeTypeName, propertyName,
				propertyType, isAutoCreated, isMandatory, isProtected, isMultiple, onParentVersionAction, index);
	}
	
    JSONObject getJSONObject(){
    	return jsonObj;
    }
}
