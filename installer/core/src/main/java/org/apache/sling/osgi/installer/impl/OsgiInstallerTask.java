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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for tasks that can be executed by the {@link OsgiInstallerImpl}
 */
public abstract class OsgiInstallerTask implements Comparable<OsgiInstallerTask> {

    private final EntityResourceList entityResourceList;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public OsgiInstallerTask(final EntityResourceList erl) {
        this.entityResourceList = erl;
    }

    /**
     * Return the corresponding resource - depending on the task this might be null.
     */
    public RegisteredResource getResource() {
        if ( this.entityResourceList != null ) {
            return this.entityResourceList.getActiveResource();
        }
        return null;
    }

    /**
     * Return the corresponding resource - depending on the task this might be null.
     */
    public EntityResourceList getEntityResourceList() {
        return this.entityResourceList;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public abstract void execute(OsgiInstallerContext ctx);

	/** Tasks are sorted according to this key */
	public abstract String getSortKey();

	/** All comparisons are based on getSortKey() */
	public final int compareTo(OsgiInstallerTask o) {
		return getSortKey().compareTo(o.getSortKey());
	}

	public void setFinishedState(final RegisteredResource.State state) {
	    this.entityResourceList.setFinishState(state);
	}

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + this.getResource();
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