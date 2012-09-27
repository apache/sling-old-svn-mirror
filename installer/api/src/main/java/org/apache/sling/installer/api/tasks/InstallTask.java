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
 * Base class for tasks that can be executed by the
 * {@link org.apache.sling.installer.api.OsgiInstaller}.
 *
 * The task is invoked by the installer through the {@link #execute(InstallationContext)}
 * method. During execution the task should use the {@link #setFinishedState(ResourceState)}
 * or {@link #setFinishedState(ResourceState, String)} method once the task is
 * performed or the task decided that the task can never be performed.
 *
 * If the task needs to be retried, the implementation should just not alter the
 * state at all. The installer will invoke the tasks at a later time again for
 * retrying.
 */
public abstract class InstallTask implements Comparable<InstallTask> {

    /**
     * Attribute which is set by the OSGi installer for asynchronous execution.
     * The value of the attribute is an Integer which is increased on each async call,
     * it starts with the value <code>1</code>.
     */
    public static final String ASYNC_ATTR_NAME = "org.apache.sling.installer.api.tasks.ASyncInstallTask";

    /** The resource group this task is working on. */
    private final TaskResourceGroup resourceGroup;

    /**
     * Constructor for the task
     * @param erl The resource group or <code>null</code>.
     */
    public InstallTask(final TaskResourceGroup erl) {
        this.resourceGroup = erl;
    }

    /**
     * Return the corresponding resource - depending on the task this might be null.
     */
    public TaskResource getResource() {
        if ( this.resourceGroup != null ) {
            return this.resourceGroup.getActiveResource();
        }
        return null;
    }

    /**
     * Return the corresponding resource - depending on the task this might be null.
     */
    public TaskResourceGroup getResourceGroup() {
        return this.resourceGroup;
    }

    /**
     * This is the heart of the task - it performs the actual task.
     * @param ctx The installation context.
     */
    public abstract void execute(InstallationContext ctx);

	/**
	 * Tasks are sorted according to this key.
	 * Therefore this key must uniquely identify this task.
	 * A typical sort key contains the entity id of the resource
	 * in execution.
	 */
	public abstract String getSortKey();

	/**
	 * Set the finished state for the resource.
	 * @param state The new state.
	 */
	public void setFinishedState(final ResourceState state) {
	    if ( this.resourceGroup != null ) {
	        this.resourceGroup.setFinishState(state);
	    }
	}

    /**
     * Set the finished state for the resource and the alias
     * @param state The new state.
     * @param alias The new alias
     * @since 1.1
     */
    public void setFinishedState(final ResourceState state, final String alias) {
        if ( this.resourceGroup != null ) {
            this.resourceGroup.setFinishState(state, alias);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + this.getResource();
    }

    @Override
	public final boolean equals(Object o) {
		if (o instanceof InstallTask) {
			return getSortKey().equals(((InstallTask)o).getSortKey());
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return getSortKey().hashCode();
	}

    /**
     * All comparisons are based on getSortKey().
     */
    public final int compareTo(final InstallTask o) {
        return getSortKey().compareTo(o.getSortKey());
    }

    /**
     * If this an asynchronous task it should return <code>true</code>
     * The OSGi installer will set the attribute {@link #ASYNC_ATTR_NAME}
     * with an integer value.
     * The next time, after the asynchronous task has been run and
     * the OSGi installer has restarted, this attribute will be set
     * on the resource.
     *
     * Asynchronous tasks should only be used for tasks which require
     * the OSGi installer to stop and force it to restart, like
     * a bundle update of the installer itself or a system update.
     * The OSGi installer stops itself for an asynchronous task and
     * is not able to restart itself!
     *
     * @return If this is a async request, <code>true</code>
     *         otherwise <code>false</code>
     * @since 1.3
     */
    public boolean isAsynchronousTask() {
        return false;
    }
}