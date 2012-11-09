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



import java.util.HashMap;
import java.util.Map;

/**
 * A very simple class that avoids needing Guava as a dependency (aka Google Collections)
 *
 */
public class MapBuilder {

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> getMap(Object ... kv) {
		Map<K,V> m = new HashMap<K, V>();
		for ( int i = 0; i < kv.length; i+=2 ) {
			m.put((K)kv[i], (V)kv[i+1]);
		}
		return m;
	}

}
