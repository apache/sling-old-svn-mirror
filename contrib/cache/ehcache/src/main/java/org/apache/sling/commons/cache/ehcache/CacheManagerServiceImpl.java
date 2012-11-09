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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.ManagementService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.cache.api.Cache;
import org.apache.sling.commons.cache.api.CacheManagerService;
import org.apache.sling.commons.cache.impl.AbstractCacheManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>CacheManagerServiceImpl</code>
 */
@Component(immediate = true, metatype = true)
@Service(value = CacheManagerService.class)
public class CacheManagerServiceImpl extends AbstractCacheManagerService {

	public static final String DEFAULT_CACHE_CONFIG = "sling/ehcacheConfig.xml";

	@Property(value = DEFAULT_CACHE_CONFIG)
	public static final String CACHE_CONFIG = "cache-config";

	@Property(value = "The Apache Software Foundation")
	static final String SERVICE_VENDOR = "service.vendor";

	@Property(value = "Cache Manager Service Implementation")
	static final String SERVICE_DESCRIPTION = "service.description";

	@Property()
	public static final String BIND_ADDRESS = "bind-address";

	@Property(value = "sling/ehcache/data")
	public static final String CACHE_STORE = "cache-store";

	private static final String CONFIG_PATH = "org/apache/sling/commons/cache/ehcache/ehcacheConfig.xml";

	private static final Logger LOGGER = LoggerFactory
			.getLogger(CacheManagerServiceImpl.class);
	private CacheManager cacheManager;
	private Map<String, Cache<?>> caches = new ConcurrentHashMap<String, Cache<?>>();

	public CacheManagerServiceImpl() throws IOException {
	}

	@Activate
	public void activate(Map<String, Object> properties)
			throws FileNotFoundException, IOException {
		String config = toString(properties.get(CACHE_CONFIG),
				DEFAULT_CACHE_CONFIG);
		File configFile = new File(config);
		if (configFile.exists()) {
			LOGGER.info("Configuring Cache from {} ",
					configFile.getAbsolutePath());
			InputStream in = null;
			try {
				in = processConfig(new FileInputStream(configFile), properties);
				cacheManager = new CacheManager(in);
			} finally {
				if (in != null) {
					in.close();
				}
			}
		} else {
			LOGGER.info("Configuring Cache from Classpath Default {} ",
					CONFIG_PATH);
			InputStream in = processConfig(this.getClass().getClassLoader()
					.getResourceAsStream(CONFIG_PATH), properties);
			if (in == null) {
				throw new IOException(
						"Unable to open config at classpath location "
								+ CONFIG_PATH);
			}
			cacheManager = new CacheManager(in);
			in.close();
		}

		final WeakReference<CacheManagerServiceImpl> ref = new WeakReference<CacheManagerServiceImpl>(
				this);
		/*
		 * Add in a shutdown hook, for safety
		 */
		Runtime.getRuntime().addShutdownHook(new Thread() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Thread#run()
			 */
			@Override
			public void run() {
				try {
					CacheManagerServiceImpl cm = ref.get();
					if (cm != null) {
						cm.deactivate();
					}
				} catch (Throwable t) {
					LOGGER.debug(t.getMessage(), t);
				}
			}
		});

		// register the cache manager with JMX
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		ManagementService.registerMBeans(cacheManager, mBeanServer, true, true,
				true, true);

	}


	/**
	 * perform a shutdown
	 */
	@Deactivate
	public void deactivate() {
		if (cacheManager != null) {
			cacheManager.shutdown();
			cacheManager = null;
		}
	}


	/**
	 * @param name
	 * @return
	 */
	@Override
	protected <V> Cache<V> getInstanceCache(String name) {
		if (name == null) {
			return new CacheImpl<V>(cacheManager, null);
		} else {
			@SuppressWarnings("unchecked")
			Cache<V> c = (Cache<V>) caches.get(name);
			if (c == null) {
				c = new CacheImpl<V>(cacheManager, name);
				caches.put(name, c);
			}
			return c;
		}
	}



}
