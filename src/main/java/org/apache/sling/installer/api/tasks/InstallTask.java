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
 */
public abstract class InstallTask implements Comparable<InstallTask> {

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

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + this.getResource();
    }

    @Override
	public final boolean equals(Object o) {
		if(o instanceof InstallTask) {
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
    public final int compareTo(InstallTask o) {
        return getSortKey().compareTo(o.getSortKey());
    }
}