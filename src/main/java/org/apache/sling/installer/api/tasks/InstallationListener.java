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
 * Optional listener which can be used to monitor the activities
 * of the installer.
 *
 * @since 1.2
 */
public interface InstallationListener {

    /**
     * A resource has been processed
     * The state of the resource can be queried to see whether
     * the resource has been installed or removed.
     * @param resource The resource
     */
    void processed(final TaskResource resource);

    /**
     * Indication that the installer starts a new cycle.
     * Several starting methods might be invoked one after
     * the other without a processed or suspended inbetween.
     */
    void started();

    /**
     * The installer is suspended and has processed all current
     * resources.
     */
    void suspended();
}
