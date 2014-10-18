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
package org.apache.sling.event.impl.jobs.config;

/**
 * Listener interface to get topology / queue changes.
 * Components interested in configuration changes can subscribe
 * themselves using the {@link JobManagerConfiguration}.
 */
public interface ConfigurationChangeListener {

    /**
     * Notify about a configuration change.
     * @param active {@code true} if job processing is active, otherwise {@code false}
     */
    void configurationChanged(final boolean active);
}
