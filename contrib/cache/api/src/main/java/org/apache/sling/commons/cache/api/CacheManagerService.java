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

/**
 * The Cache manager provides access to all caches in the system. Caches are
 * scoped by CacheScope and those that are bound can be unbound.
 */
public interface CacheManagerService {

	/**
	 * Get a cache to contain a specified type, with a defined scope. Getting a
	 * cache of the same name in the same scope will return the same cache for
	 * that scope. The thread invoking the method forms part of the scope for
	 * CacheScopes THREAD or REQUEST.
	 * 
	 * @param <T>
	 *            The type of the elements, but be serializable for any non
	 *            thread bound cache.
	 * @param name
	 *            the name of the cache.
	 * @param scope
	 *            the scope of the cache.
	 * @return the cache suitable for holding the type T
	 */
	<T> Cache<T> getCache(String name, CacheScope scope);

	/**
	 * Unbind the the context specified in scope.
	 * 
	 * @param scope
	 */
	void unbind(CacheScope scope);
}
