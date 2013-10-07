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
package org.apache.sling.event;

import java.util.Collection;
import java.util.Map;

import org.osgi.service.event.Event;

import aQute.bnd.annotation.ProviderType;

/**
 * This service provides the current timed events status.
 * @deprecated Use scheduled jobs instead
 */
@Deprecated
@ProviderType
public interface TimedEventStatusProvider {

    /**
     * This is a unique identifier which can be used to cancel the job.
     */
    String PROPERTY_EVENT_ID = "slingevent:eventId";

    /**
     * Return a list of currently scheduled events.
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param filterProps A list of filter property maps. Each map acts like a template. The searched event
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A non null collection.
     */
    Collection<Event> getScheduledEvents(String topic, Map<String, Object>... filterProps);

    /**
     * Return the scheduled event with the given id.
     * @return The scheduled event or null.
     */
    Event getScheduledEvent(String topic, String eventId, String jobId);

    /**
     * Cancel this timed event.
     * @param jobId The unique identifier as found in the property {@link #PROPERTY_EVENT_ID}.
     */
    void cancelTimedEvent(String jobId);
}
