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
package org.apache.sling.jcr.contentloader;

import aQute.bnd.annotation.ConsumerType;


/**
 * Encapsulates the options for the content import. 
 */
@ConsumerType
public abstract class ImportOptions {

	/**
	 * Specifies whether imported nodes should overwrite existing nodes.
	 * NOTE: this means the existing node will be deleted and a new node 
	 * will be created in the same location.
	 * @return true to overwrite nodes, false otherwise
	 */
	public abstract boolean isOverwrite();

	/**
	 * Specifies whether imported properties should overwrite existing properties.
	 * @return true to overwrite node properties, false otherwise
	 */
	public abstract boolean isPropertyOverwrite();

	/**
	 * Specifies whether versionable nodes is automatically checked in at the
	 * end of the import operation.
	 * @return true to checkin the versionable nodes, false otherwise
	 */
	public abstract boolean isCheckin();

	/**
	 * Specifies whether versionable nodes is automatically checked out when
	 * necessary.
	 * @return true to checkout the versionable nodes, false otherwise
     * @since 2.1.4
	 */
	public boolean isAutoCheckout() {
		return true;
	}

	/**
	 * Check if the content reader for the given file extension should
	 * be ignored.
	 * 
	 * @param extension the extension to check
	 * @return true to ignore the reader, false otherwise
	 */
	public abstract boolean isIgnoredImportProvider(String extension);

}