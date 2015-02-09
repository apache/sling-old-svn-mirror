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

import org.apache.sling.query.api.internal.Option;

public class SliceIterator<T> extends AbstractIterator<Option<T>> {

	private final Iterator<Option<T>> iterator;

	private final int from;

	private final int to;

	private int current;

	public SliceIterator(Iterator<Option<T>> iterator, int from, int to) {
		this.iterator = iterator;
		this.current = -1;
		this.from = from;
		this.to = to;
	}

	public SliceIterator(Iterator<Option<T>> iterator, int from) {
		this(iterator, from, Integer.MAX_VALUE);
	}

	@Override
	protected Option<T> getElement() {
		if (current > to) {
			return null;
		}

		if (iterator.hasNext()) {
			Option<T> element = iterator.next();
			if (element.isEmpty()) {
				return element;
			}
			if (++current >= from && current <= to) {
				return element;
			} else {
				return Option.empty(element.getArgumentId());
			}
		}
		return null;
	}
}