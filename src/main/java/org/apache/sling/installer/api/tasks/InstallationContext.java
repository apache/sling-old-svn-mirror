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
package org.apache.sling.installer.api.tasks;

/**
 * Context for the installation tasks.
 *
 * The context is passed into an {@link InstallTask} during
 * execution of a task. The task can schedule tasks to the
 * current or next execution cycle.
 *
 * In addition access to an audit log is provided. The task
 * should make an entry into the log each time a task has
 * succeeded like something has been installed, updated or
 * deleted.
 */
public interface InstallationContext {

	/**
	 * Schedule a task for execution in the current cycle.
	 */
	void addTaskToCurrentCycle(InstallTask t);

	/**
	 * Schedule a task for execution in the next cycle,
	 * usually to indicate that a task must be retried
	 * or the current task is finished and another task
	 * has to be run.
	 * @deprecated
	 */
	@Deprecated
    void addTaskToNextCycle(InstallTask t);

	/**
	 * Make an entry into the audit log - this should be invoked
	 * by the tasks whenever something has been installed/uninstalled etc.
	 */
	void log(String message, Object... args);

    /**
     * Add an async task.
     * This adds a task for asynchronous execution.
     * @since 1.2.0
     * @deprecated A async task should return <code>true</code> for {@link InstallTask#isAsynchronousTask()}
     *             and be add with {@link #addTaskToCurrentCycle(InstallTask)}
     */
	@Deprecated
    void addAsyncTask(InstallTask t);

    /**
     * Inform the installer about a failed async task.
     * This will restart the OSGi installer.
     *
     * This will also remove the {@link InstallTask#ASYNC_ATTR_NAME}
     * attribute from the resource.
     *
     * @since 1.3.0
     */
    void asyncTaskFailed(InstallTask t);
}
