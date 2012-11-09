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

package org.apache.sling.commons.cache.api;

import java.util.Collection;

/**
 * A Cache managed by the cache manager.
 */
public interface Cache<V> {

	/**
	 * Cache an object
	 * 
	 * @param key
	 *            The key with which to find the object.
	 * @param payload
	 *            The object to cache.
	 * @param duration
	 *            The time to cache the object (seconds).
	 */
	V put(String key, V payload);

	/**
	 * Test for a non expired entry in the cache.
	 * 
	 * @param key
	 *            The cache key.
	 * @return true if the key maps to a non-expired cache entry, false if not.
	 */
	boolean containsKey(String key);

	/**
	 * Get the non expired entry, or null if not there (or expired)
	 * 
	 * @param key
	 *            The cache key.
	 * @return The payload, or null if the payload is null, the key is not
	 *         found, or the entry has expired (Note: use containsKey() to
	 *         remove this ambiguity).
	 */
	V get(String key);

	/**
	 * Clear all entries.
	 */
	void clear();

	/**
	 * Remove this entry from the cache.
	 * 
	 * @param key
	 *            The cache key.
	 */
	boolean remove(String key);

	/**
	 * Remove the key and any child keys from the cache, this is an expensive
	 * operation.
	 * 
	 * @param key
	 */
	void removeChildren(String key);

	/**
	 * @return
	 */
	Collection<V> values();

	/**
	 * @return
	 */
	Collection<String> keys();

}
