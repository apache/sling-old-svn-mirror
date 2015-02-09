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

public class ArgumentResettingIterator<T> implements Iterator<Option<T>> {

	private final Iterator<Option<T>> iterator;

	private int index;

	public ArgumentResettingIterator(Iterator<Option<T>> iterator) {
		this.iterator = iterator;
		this.index = 0;
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public Option<T> next() {
		return Option.of(iterator.next().getElement(), index++);
	}

	@Override
	public void remove() {
		iterator.remove();
	}

}
