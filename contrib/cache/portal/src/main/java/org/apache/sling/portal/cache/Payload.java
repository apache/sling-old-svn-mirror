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

package org.apache.sling.portal.cache;


/**
 * A simple payload wrapper for the cache to record when it expires.
 *
 * @param <V>
 */
public class Payload<V> {

	private long ttiEnd;
	private long ttlEnd;
	private long tti;
	private V payload;

	/**
	 * Create the payload
	 * @param payload the payload itself
	 * @param timeToIdleSeconds how many seconds the payload can be idle for before it becomes invalid.
	 * @param timeToLiveSeconds how many seconds the payload can be live for.
	 */
	public Payload(V payload, long timeToIdleSeconds,
			long timeToLiveSeconds) {
		this.payload = payload;
		long t = System.currentTimeMillis();
		tti = timeToIdleSeconds+1000;
		ttiEnd = t+tti;
		ttlEnd = t+timeToLiveSeconds*1000;
	}

	/**
	 * @return the payload if its still valid.
	 */
	public V get() {
		long t = System.currentTimeMillis();
		if ( t > ttiEnd ) {
			return null;
		}
		if ( t > ttlEnd ) {
			return null;
		}
		ttiEnd = t + tti;
		return payload;
	}

}
