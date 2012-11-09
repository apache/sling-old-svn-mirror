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

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;


import javax.inject.Inject;

import org.apache.sling.commons.cache.api.Cache;
import org.apache.sling.commons.cache.api.CacheManagerService;
import org.apache.sling.commons.cache.api.CacheScope;
import org.apache.sling.test.AbstractOSGiRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.framework.BundleContext;

/**
 * Spin the cache up in a container to verify we have a working system.
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class) // we want to spin up the container once only.
public class CacheIT extends AbstractOSGiRunner {


	@Inject
	private CacheManagerService cacheManagerService;
	
	@Inject
	private BundleContext bundleContext;
		

	@Override
	protected String getImports() {
		return "org.apache.sling.commons.cache.api,javax.transaction,javax.transaction.xa";
	}
	

	@Override
	public Option[] getOptions() {
		// check();
		return options(
				provision(
						// Infinispan needs a transaction API.
			            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
						mavenBundle("org.apache.sling", "org.apache.sling.commons.cache.api")
				));
	}


	@Override
	protected String getArtifactName() {
		return "org.apache.sling.commons.cache.infinispan";
	}


	@Test
	public void testCacheManagerReplicated() {
		check();
		String cacheName = "string-cache-replicated";
		Assert.assertNotNull(cacheManagerService);
		Cache<String> c = cacheManagerService.getCache(cacheName,
				CacheScope.CLUSTERREPLICATED);
		Assert.assertNotNull(c);
		String v = cacheName + String.valueOf(System.currentTimeMillis());
		c.put("key", v);
		Assert.assertEquals(v, c.get("key"));
		cacheManagerService.unbind(CacheScope.CLUSTERREPLICATED);
		cacheManagerService.unbind(CacheScope.CLUSTERINVALIDATED);
		cacheManagerService.unbind(CacheScope.INSTANCE);
		cacheManagerService.unbind(CacheScope.REQUEST);
		cacheManagerService.unbind(CacheScope.THREAD);
		c = cacheManagerService.getCache(cacheName,
				CacheScope.CLUSTERREPLICATED);
		Assert.assertNotNull(c);
		Assert.assertEquals(v, c.get("key"));

	}

	@Test
	public void testCacheManagerInvalidated() {
		String cacheName = "string-cache-invalidated";
		Assert.assertNotNull(cacheManagerService);
		Cache<String> c = cacheManagerService.getCache(cacheName,
				CacheScope.CLUSTERINVALIDATED);
		Assert.assertNotNull(c);
		String v = cacheName + String.valueOf(System.currentTimeMillis());
		c.put("key", v);
		Assert.assertEquals(v, c.get("key"));
		cacheManagerService.unbind(CacheScope.CLUSTERREPLICATED);
		cacheManagerService.unbind(CacheScope.CLUSTERINVALIDATED);
		cacheManagerService.unbind(CacheScope.INSTANCE);
		cacheManagerService.unbind(CacheScope.REQUEST);
		cacheManagerService.unbind(CacheScope.THREAD);
		c = cacheManagerService.getCache(cacheName,
				CacheScope.CLUSTERINVALIDATED);
		Assert.assertNotNull(c);
		Assert.assertEquals(v, c.get("key"));
	}



	@Test
	public void testCacheManagerInstance() {
		String cacheName = "string-cache-instance";
		Assert.assertNotNull(cacheManagerService);
		Cache<String> c = cacheManagerService.getCache(cacheName,
				CacheScope.INSTANCE);
		Assert.assertNotNull(c);
		String v = cacheName + String.valueOf(System.currentTimeMillis());
		c.put("key", v);
		Assert.assertEquals(v, c.get("key"));
		cacheManagerService.unbind(CacheScope.CLUSTERREPLICATED);
		cacheManagerService.unbind(CacheScope.CLUSTERINVALIDATED);
		cacheManagerService.unbind(CacheScope.INSTANCE);
		cacheManagerService.unbind(CacheScope.REQUEST);
		cacheManagerService.unbind(CacheScope.THREAD);
		c = cacheManagerService.getCache(cacheName, CacheScope.INSTANCE);
		Assert.assertNotNull(c);
		Assert.assertEquals(v, c.get("key"));
	}

	@Test
	public void testCacheManagerRequest() {
		String cacheName = "string-cache-request";
		Assert.assertNotNull(cacheManagerService);
		Cache<String> c = cacheManagerService.getCache(cacheName,
				CacheScope.REQUEST);
		Assert.assertNotNull(c);
		String v = cacheName + String.valueOf(System.currentTimeMillis());
		c.put("key", v);
		Assert.assertEquals(v, c.get("key"));
		cacheManagerService.unbind(CacheScope.CLUSTERREPLICATED);
		cacheManagerService.unbind(CacheScope.CLUSTERINVALIDATED);
		cacheManagerService.unbind(CacheScope.INSTANCE);
		cacheManagerService.unbind(CacheScope.REQUEST);
		cacheManagerService.unbind(CacheScope.THREAD);
		c = cacheManagerService.getCache(cacheName, CacheScope.REQUEST);
		Assert.assertNotNull(c);
		Assert.assertNull(c.get("key"));
	}

	@Test
	public void testCacheManagerThread() {
		String cacheName = "string-cache-thread";
		Assert.assertNotNull(cacheManagerService);
		Cache<String> c = cacheManagerService.getCache(cacheName,
				CacheScope.THREAD);
		Assert.assertNotNull(c);
		String v = cacheName + String.valueOf(System.currentTimeMillis());
		c.put("key", v);
		Assert.assertEquals(v, c.get("key"));
		cacheManagerService.unbind(CacheScope.CLUSTERREPLICATED);
		cacheManagerService.unbind(CacheScope.CLUSTERINVALIDATED);
		cacheManagerService.unbind(CacheScope.INSTANCE);
		cacheManagerService.unbind(CacheScope.REQUEST);
		cacheManagerService.unbind(CacheScope.THREAD);
		c = cacheManagerService.getCache(cacheName, CacheScope.THREAD);
		Assert.assertNotNull(c);
		Assert.assertNull(c.get("key"));
	}

}
