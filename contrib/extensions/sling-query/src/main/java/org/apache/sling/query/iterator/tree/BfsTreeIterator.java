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

package org.apache.sling.query.iterator.tree;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.sling.query.api.internal.TreeProvider;
import org.apache.sling.query.iterator.AbstractIterator;

public class BfsTreeIterator<T> extends AbstractIterator<T> {

	private final Deque<T> queue = new LinkedList<T>();

	private final TreeProvider<T> provider;

	private Iterator<T> currentIterator;

	public BfsTreeIterator(T root, TreeProvider<T> provider) {
		this.currentIterator = provider.listChildren(root);
		this.provider = provider;
	}

	@Override
	protected T getElement() {
		if (currentIterator.hasNext()) {
			T resource = currentIterator.next();
			queue.add(resource);
			return resource;
		}

		if (!queue.isEmpty()) {
			currentIterator = provider.listChildren(queue.pop());
			return getElement();
		}

		return null;
	}
}
