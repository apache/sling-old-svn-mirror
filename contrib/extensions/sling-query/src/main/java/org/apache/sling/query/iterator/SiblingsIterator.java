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

package org.apache.sling.query.iterator;

import java.util.Iterator;
import java.util.ListIterator;

import org.apache.sling.query.api.Predicate;
import org.apache.sling.query.api.internal.TreeProvider;
import org.apache.sling.query.util.IteratorUtils;
import org.apache.sling.query.util.LazyList;

public class SiblingsIterator<T> extends AbstractIterator<T> {

	private final Predicate<T> until;

	private final ListIterator<T> siblings;

	private final Type type;

	private final TreeProvider<T> provider;

	private boolean finished;

	public SiblingsIterator(Predicate<T> until, T resource, Type type, TreeProvider<T> provider) {
		this.provider = provider;
		this.until = until;
		this.siblings = getRewindedIterator(resource, type);
		this.finished = false;
		this.type = type;
	}

	@Override
	protected T getElement() {
		if (finished) {
			return null;
		}
		while (type.canAdvance(siblings)) {
			T resource = type.advance(siblings);
			if (until != null && until.accepts(resource)) {
				finished = true;
				return null;
			}
			if (until == null) {
				finished = true;
			}
			return resource;
		}
		return null;
	}

	private ListIterator<T> getRewindedIterator(T resource, Type type) {
		String resourceName = provider.getName(resource);
		T parent = provider.getParent(resource);
		Iterator<T> iterator;
		if (parent == null) {
			iterator = IteratorUtils.singleElementIterator(resource);
		} else {
			iterator = provider.listChildren(parent);
		}
		ListIterator<T> listIterator = new LazyList<T>(iterator).listIterator();
		while (listIterator.hasNext()) {
			T sibling = listIterator.next();
			if (provider.getName(sibling).equals(resourceName)) {
				break;
			}
		}
		if (type == Type.PREV) {
			listIterator.previous();
		}
		return listIterator;
	}

	public enum Type {
		NEXT {
			@Override
			public boolean canAdvance(ListIterator<?> iterator) {
				return iterator.hasNext();
			}

			@Override
			public <T> T advance(ListIterator<T> iterator) {
				return iterator.next();
			}
		},
		PREV {
			@Override
			public boolean canAdvance(ListIterator<?> iterator) {
				return iterator.hasPrevious();
			}

			@Override
			public <T> T advance(ListIterator<T> iterator) {
				return iterator.previous();
			}
		};

		public abstract boolean canAdvance(ListIterator<?> iterator);

		public abstract <T> T advance(ListIterator<T> iterator);
	}
}
