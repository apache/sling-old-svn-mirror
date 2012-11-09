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
package org.apache.sling.commons.cache.infinispan;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.apache.sling.commons.cache.api.Cache;
import org.apache.sling.commons.cache.api.CacheScope;
import org.apache.sling.commons.cache.api.ThreadBound;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CacheConfigTest {

	private CacheManagerServiceImpl cacheManagerService;

	@Before
	public void setUp() throws IOException, InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		cacheManagerService = new CacheManagerServiceImpl();
		Map<String, Object> properties = MapBuilder.getMap(CacheManagerServiceImpl.CACHE_STORE,
				(Object) "target/ehcache/store", 
				CacheManagerServiceImpl.BIND_ADDRESS, "127.0.0.1",
				CacheManagerServiceImpl.CACHE_CONFIG, "src/test/resources/testconfig/simple-ehcacheConfig.xml"
				);
		cacheManagerService.activate(properties);
	}


	@After
	public void tearDown() {
		cacheManagerService.deactivate();
	}

	private void exerciseCache(String cacheName, CacheScope scope) {
		Cache<String> cache = cacheManagerService.getCache(cacheName, scope);
		cache.put("fish", "cat");
		assertTrue("Expected element to be in cache", cache.containsKey("fish"));
		Cache<String> sameCache = cacheManagerService
				.getCache(cacheName, scope);
		assertEquals("Expected cache to work", "cat", sameCache.get("fish"));
		sameCache.put("fish", "differentcat");
		assertEquals("Expected cache value to propogate", "differentcat",
				cache.get("fish"));
		sameCache.remove("fish");
		sameCache.remove("another");
		assertNull("Expected item to be removed from cache", cache.get("fish"));
		cache.put("foo", "bar");
		cache.clear();
		assertNull("Expected cache to be empty", cache.get("foo"));
		cacheManagerService.unbind(scope);
	}

	@Test
	public void testCacheStorage() {
		for (CacheScope scope : CacheScope.values()) {
			exerciseCache("TestCache", scope);
		}
	}

	@Test
	public void testNullCacheNames() {
		for (CacheScope scope : CacheScope.values()) {
			exerciseCache(null, scope);
		}
	}

	@Test
	public void testCacheWithChildKeys() {
		for (CacheScope scope : CacheScope.values()) {
			String cacheName = "SomeTestCache";
			Cache<String> cache = cacheManagerService
					.getCache(cacheName, scope);
			cache.put("fish", "cat");
			assertTrue("Expected element to be in cache",
					cache.containsKey("fish"));
			cache.put("fish/child", "childcat");
			cache.put("fish/child/child", "childcatchild");
			Cache<String> sameCache = cacheManagerService.getCache(cacheName,
					scope);
			sameCache.removeChildren("fish/child/child");
			assertNull("Expected key to be removed",
					cache.get("fish/child/child"));
			sameCache.removeChildren("fish");
			assertNull("Expected key to be removed", cache.get("fish"));
			assertNull("Expected key to be removed", cache.get("fish/child"));
		}
	}

	@Test
	public void testThreadUnbinding() {
		ThreadBound testItem = Mockito.mock(ThreadBound.class);
		Cache<ThreadBound> threadBoundCache = cacheManagerService.getCache(
				"testCache", CacheScope.THREAD);
		threadBoundCache.put("testItem", testItem);
		threadBoundCache.remove("testItem");
		threadBoundCache.put("testItem", testItem);
		threadBoundCache.clear();
		
		Mockito.verify(testItem, Mockito.times(2)).unbind();
	}

}
