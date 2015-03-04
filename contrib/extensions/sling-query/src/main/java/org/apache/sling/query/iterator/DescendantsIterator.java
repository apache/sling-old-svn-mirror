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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.sling.query.api.internal.Option;
import org.apache.sling.query.api.internal.TreeProvider;
import org.apache.sling.query.util.LazyList;

public class DescendantsIterator<T> extends AbstractIterator<Option<T>> {

	private final Iterator<Option<T>> input;

	private Option<T> current;

	private Iterator<T> descendants;

	private final TreeProvider<T> provider;

	public DescendantsIterator(Iterator<Option<T>> input, Iterator<T> descendants, TreeProvider<T> provider) {
		this.input = input;
		this.current = null;
		this.descendants = new ArrayList<T>(new LazyList<T>(descendants)).iterator();
		this.provider = provider;
	}

	@Override
	protected Option<T> getElement() {
		if (current == null) {
			if (input.hasNext()) {
				current = input.next();
			} else {
				return null;
			}
		}
		return getDescendant();
	}

	private Option<T> getDescendant() {
		while (descendants.hasNext()) {
			T descendantCandidate = descendants.next();
			if (provider.isDescendant(current.getElement(), descendantCandidate)) {
				descendants.remove();
				return Option.of(descendantCandidate, current.getArgumentId());
			}
		}
		Option<T> result = Option.empty(current.getArgumentId());
		current = null;
		return result;
	}
}
