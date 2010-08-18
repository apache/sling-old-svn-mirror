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


/** Base class for tasks that can be executed by the {@link OsgiInstallerThread} */
public abstract class OsgiInstallerTask implements Comparable<OsgiInstallerTask> {

    public abstract void execute(OsgiInstallerContext ctx);

	protected void logExecution() {
	    Logger.logInfo("OsgiInstallerTask: executing  " + this);
	}

	/** Tasks are sorted according to this key */
	public abstract String getSortKey();

	/** All comparisons are based on getSortKey() */
	public final int compareTo(OsgiInstallerTask o) {
		return getSortKey().compareTo(o.getSortKey());
	}

	@Override
	public final boolean equals(Object o) {
		if(o instanceof OsgiInstallerTask) {
			return getSortKey().equals(((OsgiInstallerTask)o).getSortKey());
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return getSortKey().hashCode();
	}
}