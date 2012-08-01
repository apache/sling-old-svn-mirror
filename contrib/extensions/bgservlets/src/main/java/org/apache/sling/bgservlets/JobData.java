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
package org.apache.sling.bgservlets;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/** Wraps a JCR Node to store and retrieve information
 * 	about a background Job
 */
public interface JobData {
    String JOB_DATA_MIXIN = "sling:bgJobData";
    String PROP_EXTENSION = "sling:jobExtension";
    
	/** Return unique path of this data item */
	String getPath();
	
	/** OutputStream used to write the job's output,
	 * 	stored permanently under the job node. 
	 */
	OutputStream getOutputStream();
	
	/** Input stream used to replay data stored
	 * 	in the stream provided by {#link getOutputStream}
	 *  @return null if no stream stored yet
	 */
	InputStream getInputStream();
	
	/** Set a named property */
	void setProperty(String name, String value);
	
	/** Get a named property, null if non-existent */
	String getProperty(String name);
	
	/** Return this item's creation time */
	Date getCreationTime();
}
