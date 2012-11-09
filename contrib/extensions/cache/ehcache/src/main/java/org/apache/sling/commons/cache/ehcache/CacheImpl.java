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

package org.apache.sling.commons.cache.ehcache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.sling.commons.cache.api.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class CacheImpl<V> implements Cache<V> {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(CacheImpl.class);
	private String cacheName;
	private net.sf.ehcache.Cache cache;
	private long miss;
	private long hits;
	private long gets;

	/**
	 * @param cacheManager
	 * @param name
	 */
	public CacheImpl(CacheManager cacheManager, String name) {
		if (name == null) {
			cacheName = "default";
		} else {
			cacheName = name;
		}
		synchronized (cacheManager) {
			cache = cacheManager.getCache(cacheName);
			if (cache == null) {
				cacheManager.addCache(cacheName);
				cache = cacheManager.getCache(cacheName);
				if (cache == null) {
					throw new RuntimeException(
							"Failed to create Cache with name " + cacheName);
				}
			}
		}
	}

	/**
	 * {@inherit-doc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#clear()
	 */
	public void clear() {
		cache.removeAll();
	}

	/**
	 * {@inherit-doc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#containsKey(java.lang.String)
	 */
	public boolean containsKey(String key) {
		return cache.isKeyInCache(key);
	}

	/**
	 * {@inherit-doc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#get(java.lang.String)
	 */
	public V get(String key) {
		Element e = cache.get(key);
		if (e == null) {
			return stats(null);
		}
		return stats(e.getObjectValue());
	}

	/**
	 * Records stats
	 * 
	 * @param objectValue
	 * @return the Object Value
	 */
	@SuppressWarnings("unchecked")
	private V stats(Object objectValue) {
		if (objectValue == null) {
			miss++;
		} else {
			hits++;
		}
		gets++;
		if (gets % 1000 == 0) {
			long hp = (100 * hits) / gets;
			long mp = (100 * miss) / gets;
			LOGGER.info(
					"{} Cache Stats hits {} ({}%), misses {} ({}%), calls {}",
					new Object[] { cacheName, hits, hp, miss, mp, gets });
		}
		return (V) objectValue;
	}

	/**
	 * {@inherit-doc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#put(java.lang.String,
	 *      java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public V put(String key, V payload) {
		V previous = null;
		if (cache.isKeyInCache(key)) {
			Element e = cache.get(key);
			if (e != null) {
				previous = (V) e.getObjectValue();
			}
		}
		cache.put(new Element(key, payload));
		return previous;
	}

	/**
	 * {@inherit-doc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#remove(java.lang.String)
	 */
	public boolean remove(String key) {
		return cache.remove(key);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#removeChildren(java.lang.String)
	 */
	public void removeChildren(String key) {
		cache.remove(key);
		if (!key.endsWith("/")) {
			key = key + "/";
		}
		List<?> keys = cache.getKeys();
		for (Object k : keys) {
			if (((String) k).startsWith(key)) {
				cache.remove(k);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#list()
	 */
	@SuppressWarnings("unchecked")
	public Collection<V> values() {
		List<String> keys = cache.getKeys();
		List<V> values = new ArrayList<V>();
		for (String k : keys) {
			Element e = cache.get(k);
			if (e != null) {
				values.add((V) e.getObjectValue());
			}
		}
		return values;
	}


	@SuppressWarnings("unchecked")
	public Collection<String> keys() {
		return cache.getKeys();
	}

}
