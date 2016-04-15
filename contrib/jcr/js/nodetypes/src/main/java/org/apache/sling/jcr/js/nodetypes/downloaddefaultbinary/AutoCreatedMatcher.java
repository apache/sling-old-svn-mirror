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
package org.apache.sling.jcr.js.nodetypes.downloaddefaultbinary;

import javax.jcr.nodetype.PropertyDefinition;

public class AutoCreatedMatcher extends AbstractPropertyMatcher implements PropertyMatcher{

	public AutoCreatedMatcher(String[] idFields, int index){
		super.idFields = idFields;
		super.index = index;
	}
	
	@Override
	public boolean match(PropertyDefinition propertyDefinition) {
		String arrayValue = super.getArrayValue(idFields, index);
		if (arrayValue!=null){
			boolean isAutoCreated = Boolean.parseBoolean(arrayValue);
			return isAutoCreated == propertyDefinition.isAutoCreated();
		}
		return false;
	}
	
}
