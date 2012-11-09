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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.sling.commons.cache.api.Cache;
import org.apache.sling.commons.cache.api.ThreadBound;

/**
 *
 */
public class MapCacheImpl<V> extends HashMap<String, V> implements Cache<V> {

	/**
   *
   */
	private static final long serialVersionUID = -5400056532743570231L;

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#containsKey(java.lang.String)
	 */
	public boolean containsKey(String key) {
		return super.containsKey(key);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#get(java.lang.String)
	 */
	public V get(String key) {
		return super.get(key);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#remove(java.lang.String)
	 */
	public boolean remove(String key) {
		V o = super.remove(key);
		if (o instanceof ThreadBound) {
			((ThreadBound) o).unbind();
		}
		return ( o != null );
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.util.HashMap#clear()
	 */
	@Override
	public void clear() {
		for (String k : super.keySet()) {
			Object o = get(k);
			if (o instanceof ThreadBound) {
				((ThreadBound) o).unbind();
			}
		}
		super.clear();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#removeChildren(java.lang.String)
	 */
	public void removeChildren(String key) {
		super.remove(key);
		if (!key.endsWith("/")) {
			key = key + "/";
		}
		Set<String> keys = super.keySet();
		for (String k : keys) {
			if ((k).startsWith(key)) {
				super.remove(k);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.sling.commons.cache.api.Cache#list()
	 */
	public Collection<V> values() {
		return super.values();
	}

	public Collection<String> keys() {
		return super.keySet();
	}

}
