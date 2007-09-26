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
package org.apache.sling.event.impl;

import javax.jcr.RepositoryException;


/**
 * An event handler for timed events.
 *
 * scr.component inherit="true"
 * @scr.property name="event.topics" value="org/apache/sling/event/timed"
 * @scr.property name="repository.path" value="/sling/timed-events"
 */
public abstract class TimedEventHandler
    extends AbstractRepositoryEventHandler{

    /**
     * Start the repository session and add this handler as an observer
     * for new events created on other nodes.
     * @throws RepositoryException
     */
    protected void startSession() throws RepositoryException {
        super.startSession();
        this.session.getWorkspace().getObservationManager()
            .addEventListener(this, javax.jcr.observation.Event.PROPERTY_CHANGED, this.repositoryPath, true, null, null, true);
    }

}
