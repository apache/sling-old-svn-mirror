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
package org.apache.sling.osgi.installer.impl;

/** Base class for tasks that can be executed by the OsgiController */ 
public abstract class OsgiControllerTask implements Comparable<OsgiControllerTask> {
	/** Execute this task */
	public abstract void execute(OsgiControllerTaskContext ctx) throws Exception;
	
	/** Tasks are sorted according to this key */
	public abstract String getSortKey();

	/** All comparisons are based on getSortKey() */
	public final int compareTo(OsgiControllerTask o) {
		return getSortKey().compareTo(o.getSortKey());
	}
	
	/** Is it worth executing this task now? */
	public boolean isExecutable(OsgiControllerTaskContext ctx) throws Exception {
	    return true;
	}

	@Override
	public final boolean equals(Object o) {
		if(o instanceof OsgiControllerTask) {
			return getSortKey().equals(((OsgiControllerTask)o).getSortKey());
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return getSortKey().hashCode();
	}
}