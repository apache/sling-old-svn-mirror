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

package org.apache.sling.commons.cache.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.cache.api.Cache;
import org.apache.sling.commons.cache.api.CacheManagerService;
import org.apache.sling.commons.cache.api.CacheScope;

/**
 * The <code>AbstractCacheManagerService</code>
 */
public abstract class AbstractCacheManagerService implements CacheManagerService {


	private ThreadLocalCacheMap requestCacheMapHolder = new ThreadLocalCacheMap();
	private ThreadLocalCacheMap threadCacheMapHolder = new ThreadLocalCacheMap();

	public AbstractCacheManagerService() throws IOException {
	}



	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.sling.commons.cache.api.CacheManagerService#getCache(java.lang.String)
	 */
	public <V> Cache<V> getCache(String name, CacheScope scope) {
		switch (scope) {
		case INSTANCE:
			return getInstanceCache(name);
		case CLUSTERINVALIDATED:
			return getInstanceCache(name);
		case CLUSTERREPLICATED:
			return getInstanceCache(name);
		case REQUEST:
			return getRequestCache(name);
		case THREAD:
			return getThreadCache(name);
		default:
			return getInstanceCache(name);
		}
	}
	
	

	/**
	 * Generate a cache bound to the thread.
	 * 
	 * @param name
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <V> Cache<V> getThreadCache(String name) {
		Map<String, Cache<?>> threadCacheMap = threadCacheMapHolder.get();
		Cache<V> threadCache = (Cache<V>) threadCacheMap.get(name);
		if (threadCache == null) {
			threadCache = new MapCacheImpl<V>();
			threadCacheMap.put(name, threadCache);
		}
		return threadCache;
	}

	/**
	 * Generate a cache bound to the request
	 * 
	 * @param name
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <V> Cache<V> getRequestCache(String name) {
		Map<String, Cache<?>> requestCacheMap = requestCacheMapHolder.get();
		Cache<V> requestCache = (Cache<V>) requestCacheMap.get(name);
		if (requestCache == null) {
			requestCache = new MapCacheImpl<V>();
			requestCacheMap.put(name, requestCache);
		}
		return requestCache;
	}

	/**
	 * @param name
	 * @return
	 */
	protected abstract <V> Cache<V> getInstanceCache(String name);

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.sling.commons.cache.api.CacheManagerService#unbind(org.apache.sling.commons.cache.api.CacheScope)
	 */
	public void unbind(CacheScope scope) {
		switch (scope) {
		case REQUEST:
			unbindRequest();
			break;
		case THREAD:
			unbindThread();
			break;
		default:
			break;
		}
	}

	/**
   *
   */
	private void unbindThread() {
		Map<String, Cache<?>> threadCache = threadCacheMapHolder.get();
		for (Cache<?> cache : threadCache.values()) {
			cache.clear();
		}
		threadCacheMapHolder.remove();
	}

	/**
   *
   */
	private void unbindRequest() {
		Map<String, Cache<?>> requestCache = requestCacheMapHolder.get();
		for (Cache<?> cache : requestCache.values()) {
			cache.clear();
		}
		requestCacheMapHolder.remove();
	}
	

	protected String toString(Object object, String defaultValue) {
		if (object == null) {
			return defaultValue;
		}
		return String.valueOf(object);
	}

	
	protected InputStream processConfig(InputStream in,
			Map<String, Object> properties) throws IOException {
		if (in == null) {
			return null;
		}
		StringBuilder config = new StringBuilder(IOUtils.toString(in, "UTF-8"));
		in.close();
		int pos = 0;
		for (;;) {
			int start = config.indexOf("${", pos);
			if (start < 0) {
				break;
			}
			int end = config.indexOf("}", start);
			if (end < 0) {
				throw new IllegalArgumentException(
						"Config file malformed, unterminated variable "
								+ config.substring(start,
										Math.min(start + 10, config.length())));
			}
			String key = config.substring(start + 2, end);
			if (properties.containsKey(key)) {
				String replacement = (String) properties.get(key);
				config.replace(start, end + 1, replacement);
				pos = start + replacement.length();
			} else {
				throw new IllegalArgumentException(
						"Missing replacement property " + key);
			}
		}
		return new ByteArrayInputStream(config.toString().getBytes("UTF-8"));

	}


}
