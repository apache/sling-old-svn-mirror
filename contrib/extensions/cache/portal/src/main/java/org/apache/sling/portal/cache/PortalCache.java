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

import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.pluto.container.PortletWindow;
import org.apache.sling.commons.cache.api.CacheManagerService;
import org.apache.sling.commons.cache.api.CacheScope;
import org.apache.sling.portal.container.cache.Cache;
import org.apache.sling.portal.container.cache.CacheValue;

/**
 * Implementation of the Portals Cache service using the Cache API.
 *
 */
@Component(immediate=true, metatype=true)
@Service
public class PortalCache implements Cache {
	
	private static final long DEFAULT_TTL = 120;
	private static final long DEFAULT_TTI = 30;
	private static final String DEFAULT_CACHE_NAME = "portal";
	
	
	@Property(longValue=DEFAULT_TTL)
	private static String CONF_TIME_TO_LIVE = "time-to-live";
	@Property(longValue=DEFAULT_TTI)
	private static String CONF_TIME_TO_IDLE = "time-to-idle";
	@Property(value=DEFAULT_CACHE_NAME)
	private static String CONF_CACHE_NAME = "cache-name";
	

	@Reference
	private CacheManagerService cacheManagerService;
	private org.apache.sling.commons.cache.api.Cache<Payload<CacheValue>> cache;
	private long timeToLive = DEFAULT_TTL;
	private long timeToIdle = DEFAULT_TTI;
	private String cacheName = DEFAULT_CACHE_NAME;
	
	@Activate
	public void activate(Map<String, Object> properties) {
		cacheName = get(CONF_CACHE_NAME, properties, DEFAULT_CACHE_NAME);
		timeToLive = get(CONF_TIME_TO_LIVE, properties, DEFAULT_TTL);
		timeToIdle = get(CONF_TIME_TO_IDLE, properties, DEFAULT_TTI);
		cache = cacheManagerService.getCache(cacheName, CacheScope.CLUSTERINVALIDATED);
	}
	
	private <V> V get(String key, Map<String, Object> properties,
			V defaultValue) {
		@SuppressWarnings("unchecked")
		V v = (V) properties.get(key);
		if ( v == null ) {
			return defaultValue;
		}
		return v;
	}

	@Deactivate 
	public void deactivate(Map<String, Object> properties) {
	}

	public CacheValue getCacheEntry(String key) {
		Payload<CacheValue> h = cache.get(key);
		if ( h == null ) {
			return null;
		}
		return h.get();
	}

	public void putCacheEntry(String key, CacheValue cachedResponse) {
		cache.put(key, new Payload<CacheValue>(cachedResponse, getTimeToIdleSeconds(), getTimeToLiveSeconds()));
	}

	public boolean removeCacheEntry(String key) {
		return cache.remove(key);
	}

	public Collection<String> getKeys() {
		return cache.keys();
	}

	public void clearCache() {
		cache.clear();
	}

	public String createCacheKey(PortletWindow portletWindow,
			HttpServletRequest request) {
		// FIXME: Check that this gives the correct isolation to items in the cache.
		// Keys should be the same regardless of the app server instance.
		// ignoring the user.
		return request.getRequestURI()+":"+portletWindow.getId();
	}

	public long getTimeToIdleSeconds() {
		return timeToIdle;
	}

	public long getTimeToLiveSeconds() {
		return timeToLive;
	}

}
