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

package org.apache.sling.api.request;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

/**
 * represents an event published by the Sling engine while
 * dispatching a request.
 * <p>
 * This class is not intended to be extended or instantiated by clients.
 *
 * @see org.apache.sling.api.request.SlingRequestListener
 * @since 2.1.0
*/
public class SlingRequestEvent {

	private final ServletContext sc;
	private final ServletRequest request;
	private final EventType type;

	/**
	 * type of the event
	 */
	public enum EventType { EVENT_INIT, EVENT_DESTROY };

	public SlingRequestEvent (ServletContext sc, ServletRequest request, EventType type ) {
		this.sc = sc;
		this.request = request;
		this.type = type;
	}

	/**
	 * Gets the actual servlet context object as <code>ServletContext</code>
	 * @return the actual servlet context.
	 */
	public ServletContext getServletContext() {
		return sc;
	}

	/**
	 * Gets the actual request object as <code>ServletRequest</code>
	 * @return the actual request object as <code>ServletRequest</code>
	 */
	public ServletRequest getServletRequest() {
		return request;
	}

	/**
	 * get the type of the event, eg. EVENT_INIT or EVENT_DESTROY
	 * @return the type of the event as <code>EventType</code>,
	 * eg. EVENT_INIT or EVENT_DESTROY
	 */
	public EventType getType () {
		return type;
	}
}
