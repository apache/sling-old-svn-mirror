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

package org.apache.sling.query.selector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.query.api.Function;
import org.apache.sling.query.api.Predicate;
import org.apache.sling.query.api.SearchStrategy;
import org.apache.sling.query.api.internal.IteratorToIteratorFunction;
import org.apache.sling.query.api.internal.Option;
import org.apache.sling.query.api.internal.TreeProvider;
import org.apache.sling.query.function.CompositeFunction;
import org.apache.sling.query.function.FilterFunction;
import org.apache.sling.query.iterator.AlternativeIterator;
import org.apache.sling.query.iterator.EmptyElementFilter;
import org.apache.sling.query.iterator.SuppIterator;
import org.apache.sling.query.selector.parser.Modifier;
import org.apache.sling.query.selector.parser.Selector;
import org.apache.sling.query.selector.parser.SelectorParser;
import org.apache.sling.query.selector.parser.SelectorSegment;
import org.apache.sling.query.util.IteratorUtils;
import org.apache.sling.query.util.LazyList;

public class SelectorFunction<T> implements IteratorToIteratorFunction<T>, Predicate<T> {

	private final List<IteratorToIteratorFunction<T>> selectorFunctions;

	private final TreeProvider<T> provider;

	private final SearchStrategy strategy;

	public SelectorFunction(String selector, TreeProvider<T> provider, SearchStrategy strategy) {
		this.provider = provider;
		this.strategy = strategy;
		List<Selector> selectors = SelectorParser.parse(selector);
		selectorFunctions = new ArrayList<IteratorToIteratorFunction<T>>();
		for (Selector s : selectors) {
			selectorFunctions.add(createSelectorFunction(s.getSegments()));
		}
	}

	@Override
	public Iterator<Option<T>> apply(Iterator<Option<T>> input) {
		LazyList<Option<T>> list = new LazyList<Option<T>>(input);
		List<Iterator<Option<T>>> iterators = new ArrayList<Iterator<Option<T>>>();
		for (IteratorToIteratorFunction<T> function : selectorFunctions) {
			iterators.add(new SuppIterator<T>(list, function));
		}
		return new AlternativeIterator<T>(iterators);
	}

	@Override
	public boolean accepts(T resource) {
		Iterator<Option<T>> result = apply(IteratorUtils.singleElementIterator(Option.of(resource, 0)));
		return new EmptyElementFilter<T>(result).hasNext();
	}

	private IteratorToIteratorFunction<T> createSelectorFunction(List<SelectorSegment> segments) {
		List<Function<?, ?>> segmentFunctions = new ArrayList<Function<?, ?>>();
		for (SelectorSegment segment : segments) {
			segmentFunctions.addAll(createSegmentFunction(segment));
		}
		return new CompositeFunction<T>(segmentFunctions);
	}

	private List<Function<?, ?>> createSegmentFunction(SelectorSegment segment) {
		List<Function<?, ?>> functions = new ArrayList<Function<?, ?>>();
		HierarchyOperator operator = HierarchyOperator.findByCharacter(segment.getHierarchyOperator());
		functions.add(operator.getFunction(segment, strategy, provider));
		Predicate<T> predicate = provider.getPredicate(segment.getType(), segment.getName(),
				segment.getAttributes());
		functions.add(new FilterFunction<T>(predicate));
		for (Modifier modifiers : segment.getModifiers()) {
			FunctionType type = FunctionType.valueOf(modifiers.getName().toUpperCase());
			Function<?, ?> f = type.getFunction(modifiers.getArgument(), strategy, provider);
			functions.add(f);
		}
		return functions;
	}

}
