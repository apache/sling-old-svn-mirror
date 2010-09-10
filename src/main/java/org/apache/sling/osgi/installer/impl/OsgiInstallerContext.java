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


/**
 * Context for the installation tasks.
 * Currently it allows to add a task to the current or the next cycle.
 */
public interface OsgiInstallerContext {

	/**
	 * Schedule a task for execution in the current OsgiController cycle
	 */
	void addTaskToCurrentCycle(OsgiInstallerTask t);

	/**
	 * Schedule a task for execution in the next OsgiController cycle,
	 * usually to indicate that a task must be retried
	 */
	void addTaskToNextCycle(OsgiInstallerTask t);

	/**
	 * Make an entry into the audit log - this should be invoked
	 * by the tasks whenever something has been installed/uninstalled etc.
	 */
	void log(String message, Object... args);
}
