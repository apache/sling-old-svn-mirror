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

package org.apache.sling.query.function;

import java.util.Iterator;

import org.apache.sling.query.api.Predicate;
import org.apache.sling.query.api.SearchStrategy;
import org.apache.sling.query.api.internal.ElementToIteratorFunction;
import org.apache.sling.query.api.internal.IteratorToIteratorFunction;
import org.apache.sling.query.api.internal.Option;
import org.apache.sling.query.api.internal.TreeProvider;
import org.apache.sling.query.iterator.EmptyElementFilter;
import org.apache.sling.query.selector.SelectorFunction;
import org.apache.sling.query.util.IteratorUtils;

public class HasFunction<T> implements ElementToIteratorFunction<T> {

	private final IteratorToIteratorFunction<T> findFunction;

	private final IteratorToIteratorFunction<T> filter;

	private HasFunction(FindFunction<T> findFunction, IteratorToIteratorFunction<T> filter) {
		this.findFunction = new IteratorToIteratorFunctionWrapper<T>(findFunction);
		this.filter = filter;
	}

	public HasFunction(String selectorString, SearchStrategy searchStrategy, TreeProvider<T> provider) {
		this(new FindFunction<T>(searchStrategy, provider, selectorString), new SelectorFunction<T>(
				selectorString, provider, searchStrategy));
	}

	public HasFunction(Predicate<T> predicate, SearchStrategy searchStrategy, TreeProvider<T> provider) {
		this(new FindFunction<T>(searchStrategy, provider, ""), new FilterFunction<T>(predicate));
	}

	public HasFunction(Iterable<T> iterable, TreeProvider<T> provider) {
		this.findFunction = new DescendantFunction<T>(iterable, provider);
		this.filter = new IdentityFunction<T>();
	}

	@Override
	public Iterator<T> apply(T input) {
		Iterator<Option<T>> iterator = IteratorUtils.singleElementIterator(Option.of(input, 0));
		iterator = findFunction.apply(iterator);
		iterator = filter.apply(iterator);
		if (new EmptyElementFilter<T>(iterator).hasNext()) {
			return IteratorUtils.singleElementIterator(input);
		} else {
			return IteratorUtils.emptyIterator();
		}
	}
}