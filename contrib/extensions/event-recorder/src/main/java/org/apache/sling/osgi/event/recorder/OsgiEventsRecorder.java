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
package org.apache.sling.osgi.event.recorder;

import java.util.Iterator;

/** Record OSGi events and give access to them for display, profiling, etc. */
public interface OsgiEventsRecorder {
	
	public static class RecordedEvent {
		public RecordedEvent(String entity, String id, String action) {
			this.entity = entity;
			this.id = id;
			this.action = action;
			this.timestamp = System.currentTimeMillis();
		}
		
		/** The OSGi "entity" (framework, bundle, service, etc.) 
		 * 	that this event refers to */
		public final String entity;

		/** The entiy ID (bundle symbolic name, etc.) */
		public final String id;
		
		/** What happened to the entity (STARTED, etc) */
		public final String action;
		
		/** Event timestamp (clock time) */
		public final long timestamp;
	}
	
	/** When the service started */
	long getStartupTimestamp();
	
	/** Timestamp of latest event (used to compute graph scales) */
	long getLastTimestamp();
	
	/** Clear the list of events and reset startup time */
	void clear();
	
	/** True if recording is enabled */
	boolean isActive();

	/** Return an Iterator on our recorded events */
	Iterator<RecordedEvent> getEvents();
}
