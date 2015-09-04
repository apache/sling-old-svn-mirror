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

import org.apache.sling.query.api.internal.Option;
import org.apache.sling.query.util.LazyList;

public class LastIterator<T> extends AbstractIterator<Option<T>> {

	private final LazyList<Option<T>> lazyList;

	private final ListIterator<Option<T>> iterator;

	private boolean finished;

	private boolean initialized;

	private int lastIndex = -1;

	public LastIterator(Iterator<Option<T>> iterator) {
		this.lazyList = new LazyList<Option<T>>(iterator);
		this.iterator = lazyList.listIterator();
	}

	@Override
	protected Option<T> getElement() {
		if (finished) {
			return null;
		}

		initializeLastIndex();

		Option<T> candidate;
		if (iterator.hasNext()) {
			candidate = iterator.next();
		} else {
			finished = true;
			return null;
		}
		if (iterator.previousIndex() == lastIndex) {
			finished = true;
			return candidate;
		} else {
			return Option.empty(candidate.getArgumentId());
		}
	}

	private void initializeLastIndex() {
		ListIterator<Option<T>> i = lazyList.listIterator();
		if (!initialized) {
			while (i.hasNext()) {
				if (!i.next().isEmpty()) {
					lastIndex = i.previousIndex();
				}
			}
		}
		initialized = true;
	}
}
