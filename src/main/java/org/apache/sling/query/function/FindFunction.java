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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.query.api.SearchStrategy;
import org.apache.sling.query.api.internal.ElementToIteratorFunction;
import org.apache.sling.query.api.internal.TreeProvider;
import org.apache.sling.query.iterator.WarningIterator;
import org.apache.sling.query.iterator.tree.BfsTreeIterator;
import org.apache.sling.query.iterator.tree.DfsTreeIterator;
import org.apache.sling.query.selector.parser.Selector;
import org.apache.sling.query.selector.parser.SelectorParser;
import org.apache.sling.query.selector.parser.SelectorSegment;

public class FindFunction<T> implements ElementToIteratorFunction<T> {

	private final List<SelectorSegment> preFilteringSelector;

	private final TreeProvider<T> provider;

	private final SearchStrategy strategy;

	public FindFunction(SearchStrategy searchStrategy, TreeProvider<T> provider,
			SelectorSegment preFilteringSelector) {
		this.strategy = searchStrategy;
		this.provider = provider;
		this.preFilteringSelector = Arrays.asList(preFilteringSelector);
	}

	public FindFunction(SearchStrategy searchStrategy, TreeProvider<T> provider, String preFilteringSelector) {
		this.strategy = searchStrategy;
		this.provider = provider;
		List<Selector> selectors = SelectorParser.parse(preFilteringSelector);
		this.preFilteringSelector = SelectorParser.getFirstSegmentFromEachSelector(selectors);
	}

	@Override
	public Iterator<T> apply(T input) {
		Iterator<T> iterator;
		switch (strategy) {
			case BFS:
				iterator = new BfsTreeIterator<T>(input, provider);
				break;
			case QUERY:
				iterator = provider.query(preFilteringSelector, input);
				break;
			case DFS:
			default:
				iterator = new DfsTreeIterator<T>(input, provider);
				break;
		}
		return new WarningIterator<T>(iterator);
	}
}