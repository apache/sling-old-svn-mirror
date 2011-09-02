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
package org.apache.sling.installer.api.event;


/**
 * Optional listener which can be used to monitor the activities
 * of the installer.
 *
 * @since 1.0
 */
public interface InstallationEvent {

    public enum TYPE {
        STARTED,
        SUSPENDED,
        PROCESSED
    };

    /**
     * Return the event type.
     */
    TYPE getType();

    /**
     * Return the source of the event.
     * For {@link TYPE#STARTED} and {@link TYPE#SUSPENDED} events
     * this is <code>null</code>.
     * For {@link TYPE#PROCESSED} events this is a
     * {@link org.apache.sling.installer.api.tasks.TaskResource}.
     */
    Object getSource();
}
