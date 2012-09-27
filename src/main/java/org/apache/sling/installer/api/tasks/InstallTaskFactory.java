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
 * The install task factory creates a task for a given
 * resource.
 * An install task factory is a plugin service that
 * checks a resource if it is installable by this plugin
 * and creates an appropriate task.
 */
public interface InstallTaskFactory {

    /**
     * Creates an {@link InstallTask} for the resource or
     * <code>null</code> if the factory does not support the resource.
     *
     * The factory should not alter the state of the resources,
     * therefore it's not allowed to call one of the setState methods
     * on the task resource group!
     */
    InstallTask createTask(final TaskResourceGroup toActivate);
}
