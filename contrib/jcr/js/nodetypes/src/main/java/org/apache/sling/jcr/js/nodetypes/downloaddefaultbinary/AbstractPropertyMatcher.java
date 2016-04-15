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

/**
 * Makes it possible to specify the array and index but retrieving it on demand lazily. 
 */
public class AbstractPropertyMatcher {
	
	protected String[] idFields;
	protected int index;

	public String getArrayValue(String[] idFields, int index) {
		if (index <= idFields.length-1){
			return idFields[index];
		}
		return null;
	}

}
