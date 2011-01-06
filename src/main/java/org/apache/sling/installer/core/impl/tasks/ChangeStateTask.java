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
package org.apache.sling.installer.core.impl.tasks;

import org.apache.sling.installer.core.impl.OsgiInstallerContext;
import org.apache.sling.installer.core.impl.OsgiInstallerTask;
import org.apache.sling.installer.core.impl.RegisteredResource;
import org.apache.sling.installer.core.impl.RegisteredResourceGroup;

/**
 * Simple general task, setting the state of a registered resource.
 */
public class ChangeStateTask extends OsgiInstallerTask {

    private static final String ORDER = "00-";

    private final RegisteredResource.State state;

    public ChangeStateTask(final RegisteredResourceGroup r,
                           final RegisteredResource.State s) {
        super(r);
        this.state = s;
    }

    /**
     * @see org.apache.sling.installer.core.impl.OsgiInstallerTask#execute(org.apache.sling.installer.core.impl.OsgiInstallerContext)
     */
    public void execute(final OsgiInstallerContext ctx) {
        this.setFinishedState(this.state);
    }

    @Override
    public String getSortKey() {
        return ORDER + getResource().getEntityId();
    }
}
