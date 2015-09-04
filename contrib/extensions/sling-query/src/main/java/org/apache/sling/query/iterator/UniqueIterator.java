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
import org.apache.sling.query.api.internal.TreeProvider;

public class UniqueIterator<T> extends AbstractIterator<Option<T>> {

	private final Iterator<Option<T>> iterator;

	private final TreeProvider<T> treeProvider;

	private T lastElement;

	public UniqueIterator(Iterator<Option<T>> input, TreeProvider<T> treeProvider) {
		this.iterator = input;
		this.treeProvider = treeProvider;
	}

	@Override
	protected Option<T> getElement() {
		if (!iterator.hasNext()) {
			return null;
		}
		Option<T> candidate = iterator.next();
		Option<T> result;
		if (treeProvider.sameElement(lastElement, candidate.getElement())) {
			result = Option.empty(candidate.getArgumentId());
		} else {
			result = candidate;
		}
		lastElement = candidate.getElement();
		return result;
	}

}
