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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Used by generators to generate the right content and by the test cases to compare if the right content has been returned.
 *
 */
public class GenerationConstants {
	
	public static final String CONSTRAINT_STRING = ".*";
	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");		
	private static Calendar maxDate = Calendar.getInstance();
	static {
		maxDate.clear();
		maxDate.set(2012, 03, 01);
	}
	public static final String CONSTRAINT_DATE = df.format(maxDate.getTime());
	public static final String CONSTRAINT_DOUBLE = "[,5]";
	public static final String CONSTRAINT_LONG = "[,55]";
	public static final String CONSTRAINT_BINARY = "[,1024]";
	public static final String CONSTRAINT_BOOLEAN = "true";
	public static final String CONSTRAINT_NAME = "myapp:myName";
	public static final String CONSTRAINT_PATH = "/myapp:myName/myapp:myChildNode/*";
	public static final String CONSTRAINT_REFERENCE = "nt:unstructured";
	
	
	public static final Calendar DEFAULT_VALUE_CALENDAR = Calendar.getInstance();
	static {
		DEFAULT_VALUE_CALENDAR.clear();
		DEFAULT_VALUE_CALENDAR.set(2012, 01, 01);	
	}
	public static final String DEFAULT_VALUE_BINARY = "/ntName/stringPropertyDef/Binary/true/true/true/true/VERSION/0.default_binary_value.bin";
	public static final String DEFAULT_VALUE_STRING = "Default-String";
	public static final String DEFAULT_VALUE_BINARY_0 = "A content";
	public static final String DEFAULT_VALUE_BINARY_1 = "An other content";
	public static final Double DEFAULT_VALUE_DOUBLE = new Double(2.2);
	public static final Long DEFAULT_VALUE_LONG = new Long(2);
	public static final Boolean DEFAULT_VALUE_BOOLEAN = Boolean.TRUE;
	public static final String DEFAULT_VALUE_NAME = "myapp:myName";
	public static final String DEFAULT_VALUE_PATH = "/myapp:myName/myapp:myChildNode/aSubChildNode";
	public static final String DEFAULT_VALUE_REFERENCE = "nt:unstructured";

	public static final String NODETYPE_REQ_PRIMARY_TYPE_NAME1 = "requiredPrimaryType1";
	public static final String NODETYPE_REQ_PRIMARY_TYPE_NAME2 = "requiredPrimaryType2";

}
