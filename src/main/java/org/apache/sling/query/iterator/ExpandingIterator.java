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

import org.apache.sling.query.api.internal.ElementToIteratorFunction;
import org.apache.sling.query.api.internal.Option;

/**
 * This iterator evaluates each element from the source iterator, expanding it using given function.
 */
public class ExpandingIterator<T> extends AbstractIterator<Option<T>> {

	private final ElementToIteratorFunction<T> function;

	private final Iterator<Option<T>> parentIterator;

	private Option<T> parentElement;

	private Iterator<T> currentIterator;

	public ExpandingIterator(ElementToIteratorFunction<T> expandingFunction,
			Iterator<Option<T>> sourceIterator) {
		this.function = expandingFunction;
		this.parentIterator = sourceIterator;
	}

	@Override
	protected Option<T> getElement() {
		if (currentIterator != null && currentIterator.hasNext()) {
			return Option.of(currentIterator.next(), parentElement.getArgumentId());
		}
		while (parentIterator.hasNext()) {
			parentElement = parentIterator.next();
			if (parentElement.isEmpty()) {
				return parentElement;
			}
			currentIterator = function.apply(parentElement.getElement());
			if (currentIterator.hasNext()) {
				return getElement();
			} else {
				return Option.empty(parentElement.getArgumentId());
			}
		}
		return null;
	}
}
