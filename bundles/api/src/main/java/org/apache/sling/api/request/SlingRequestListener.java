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

import aQute.bnd.annotation.ConsumerType;

/**
 * Implementations of this service interface receive notifications about
 * changes to Sling request of the Sling application they are part of.
 * To receive notification events, the implementation class must be
 * registered as an OSGi service with the service name
 * org.apache.sling.api.request.SlingRequestListener.
 */
@ConsumerType
public interface SlingRequestListener {

	String SERVICE_NAME = "org.apache.sling.api.request.SlingRequestListener";

	/**
	 * This method is called from the Sling application for every
	 * <code>EventType</code> appearing during the dispatching of
	 * a Sling request
	 *
	 * @param sre the object representing the event
	 *
	 * @see org.apache.sling.api.request.SlingRequestEvent.EventType
	 */
	void onEvent( SlingRequestEvent sre );
}
