/*-
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

package org.apache.sling.query.predicate;

import org.apache.sling.query.api.Predicate;
import org.apache.sling.query.api.internal.TreeProvider;
import org.apache.sling.query.util.LazyList;

public class IterableContainsPredicate<T> implements Predicate<T> {

	private final Iterable<T> iterable;

	private final TreeProvider<T> provider;

	public IterableContainsPredicate(Iterable<T> iterable, TreeProvider<T> provider) {
		this.iterable = new LazyList<T>(iterable.iterator());
		this.provider = provider;
	}

	@Override
	public boolean accepts(T element) {
		for (T t : iterable) {
			if (provider.sameElement(t, element)) {
				return true;
			}
		}
		return false;
	}
}
