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
 * Defines the scope of the cache
 */
public enum CacheScope {
	/**
	 * Bind the Cache to the request scope.
	 */
	REQUEST(),
	/**
	 * Bind a cache to the Thread, forever. WARNING: use with extreme caution,
	 * as any classes referenced in this type of cache will keep classloaders
	 * open and result in memory leaks
	 */
	THREAD(),
	/**
	 * Bind the cache to the instance, one per instance.
	 */
	INSTANCE(),
	/**
	 * Make the cache bound to the instance, but accept cluster wide
	 * invalidations.
	 */
	CLUSTERINVALIDATED(),
	/**
	 * Replicate the cache over the whole cluster.
	 */
	CLUSTERREPLICATED();

}
