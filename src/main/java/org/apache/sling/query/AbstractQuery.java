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

package org.apache.sling.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.query.api.Function;
import org.apache.sling.query.api.Predicate;
import org.apache.sling.query.api.SearchStrategy;
import org.apache.sling.query.api.internal.IteratorToIteratorFunction;
import org.apache.sling.query.api.internal.Option;
import org.apache.sling.query.api.internal.TreeProvider;
import org.apache.sling.query.function.AddFunction;
import org.apache.sling.query.function.ChildrenFunction;
import org.apache.sling.query.function.ClosestFunction;
import org.apache.sling.query.function.CompositeFunction;
import org.apache.sling.query.function.DescendantFunction;
import org.apache.sling.query.function.FilterFunction;
import org.apache.sling.query.function.FindFunction;
import org.apache.sling.query.function.HasFunction;
import org.apache.sling.query.function.IdentityFunction;
import org.apache.sling.query.function.LastFunction;
import org.apache.sling.query.function.NextFunction;
import org.apache.sling.query.function.NotFunction;
import org.apache.sling.query.function.ParentFunction;
import org.apache.sling.query.function.ParentsFunction;
import org.apache.sling.query.function.PrevFunction;
import org.apache.sling.query.function.SiblingsFunction;
import org.apache.sling.query.function.SliceFunction;
import org.apache.sling.query.function.UniqueFunction;
import org.apache.sling.query.iterator.EmptyElementFilter;
import org.apache.sling.query.iterator.OptionDecoratingIterator;
import org.apache.sling.query.iterator.OptionStrippingIterator;
import org.apache.sling.query.predicate.IterableContainsPredicate;
import org.apache.sling.query.predicate.RejectingPredicate;
import org.apache.sling.query.selector.SelectorFunction;
import org.apache.sling.query.util.LazyList;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public abstract class AbstractQuery<T, Q extends AbstractQuery<T, Q>> implements Iterable<T> {

	protected final List<Function<?, ?>> functions = new ArrayList<Function<?, ?>>();

	private final List<T> initialCollection;

	private final SearchStrategy searchStrategy;

	private final TreeProvider<T> provider;

	AbstractQuery(TreeProvider<T> provider, T[] initialCollection, SearchStrategy strategy) {
		this.provider = provider;
		this.initialCollection = new ArrayList<T>(Arrays.asList(initialCollection));
		this.searchStrategy = strategy;
	}

	protected AbstractQuery(AbstractQuery<T, Q> original, SearchStrategy searchStrategy) {
		this.functions.addAll(original.functions);
		this.initialCollection = new ArrayList<T>(original.initialCollection);
		this.searchStrategy = searchStrategy;
		this.provider = original.provider;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<T> iterator() {
		IteratorToIteratorFunction<T> f = new CompositeFunction<T>(functions);
		Iterator<Option<T>> iterator = f.apply(new OptionDecoratingIterator<T>(initialCollection.iterator()));
		iterator = new EmptyElementFilter<T>(iterator);
		return new OptionStrippingIterator<T>(iterator);
	}

	/**
	 * Include resources to the collection.
	 * 
	 * @param iterable Resources to include
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q add(T... resources) {
		return function(new AddFunction<T>(Arrays.asList(resources)));
	}

	/**
	 * Include resources to the collection.
	 * 
	 * @param iterable Resources to include
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q add(Iterable<T> iterable) {
		return function(new AddFunction<T>(iterable));
	}

	/**
	 * Transform SlingQuery collection into a lazy list.
	 * 
	 * @return List containing all elements from the collection.
	 */
	public List<T> asList() {
		return new LazyList<T>(iterator());
	}

	/**
	 * Get list of the children for each Resource in the collection.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q children() {
		return function(new ChildrenFunction<T>(provider));
	}

	/**
	 * Get list of the children for each Resource in the collection.
	 * 
	 * @param filter Children filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q children(String filter) {
		return function(new ChildrenFunction<T>(provider), filter);
	}

	/**
	 * Get list of the children for each Resource in the collection.
	 * 
	 * @param filter Children filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q children(Predicate<T> filter) {
		return function(new ChildrenFunction<T>(provider), filter);
	}

	/**
	 * Get list of the children for each Resource in the collection.
	 * 
	 * @param filter Children filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q children(Iterable<T> filter) {
		return function(new ChildrenFunction<T>(provider), filter);
	}

	/**
	 * For each Resource in the collection, return the first element matching the selector testing the
	 * Resource itself and traversing up its ancestors.
	 * 
	 * @param selector Ancestor filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q closest(String selector) {
		return closest(parse(selector));
	}

	/**
	 * For each Resource in the collection, return the first element matching the selector testing the
	 * Resource itself and traversing up its ancestors.
	 * 
	 * @param iterable Ancestor filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q closest(Iterable<T> iterable) {
		return closest(new IterableContainsPredicate<T>(iterable, provider));
	}

	/**
	 * For each Resource in the collection, return the first element matching the selector testing the
	 * Resource itself and traversing up its ancestors.
	 * 
	 * @param selector Ancestor filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q closest(Predicate<T> predicate) {
		return function(new ClosestFunction<T>(predicate, provider));
	}

	/**
	 * Reduce Resource collection to the one Resource at the given 0-based index.
	 * 
	 * @param index 0-based index
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q eq(int index) {
		return slice(index, index);
	}

	/**
	 * Filter Resource collection using given selector.
	 * 
	 * @param selector Selector
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q filter(String selector) {
		return function(new IdentityFunction<T>(), selector);
	}

	/**
	 * Filter Resource collection using given predicate object.
	 * 
	 * @param predicate Collection filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q filter(Predicate<T> predicate) {
		return function(new FilterFunction<T>(predicate));
	}

	/**
	 * Filter Resource collection using given iterable.
	 * 
	 * @param iterable Collection filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q filter(Iterable<T> iterable) {
		return function(new FilterFunction<T>(new IterableContainsPredicate<T>(iterable, provider)));
	}

	/**
	 * For each Resource in collection use depth-first search to return all its descendants. Please notice
	 * that invoking this method on a Resource being a root of a large subtree may and will cause performance
	 * problems.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q find() {
		return function(new FindFunction<T>(searchStrategy, provider, ""));
	}

	/**
	 * For each Resource in collection use breadth-first search to return all its descendants. Please notice
	 * that invoking this method on a Resource being a root of a large subtree may and will cause performance
	 * problems.
	 * 
	 * @param selector descendants filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q find(String selector) {
		return function(new FindFunction<T>(searchStrategy, provider, selector), selector);
	}

	/**
	 * For each Resource in collection use breadth-first search to return all its descendants. Please notice
	 * that invoking this method on a Resource being a root of a large subtree may and will cause performance
	 * problems.
	 * 
	 * @param predicate descendants filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q find(Predicate<T> predicate) {
		return function(new FindFunction<T>(searchStrategy, provider, ""), predicate);
	}

	/**
	 * For each Resource in collection use breadth-first search to return all its descendants. Please notice
	 * that invoking this method on a Resource being a root of a large subtree may and will cause performance
	 * problems.
	 * 
	 * @param iterable descendants filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q find(Iterable<T> iterable) {
		return function(new DescendantFunction<T>(new LazyList<T>(iterable.iterator()), provider));
	}

	/**
	 * Filter Resource collection to the first element. Equivalent to {@code eq(0)} or {@code slice(0, 0)}.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q first() {
		return eq(0);
	}

	/**
	 * Pick such Resources from the collection that have descendant matching the selector.
	 * 
	 * @param selector Descendant selector
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q has(String selector) {
		return function(new HasFunction<T>(selector, searchStrategy, provider));
	}

	/**
	 * Pick such Resources from the collection that have descendant matching the selector.
	 * 
	 * @param predicate Descendant selector
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q has(Predicate<T> predicate) {
		return function(new HasFunction<T>(predicate, searchStrategy, provider));
	}

	/**
	 * Pick such Resources from the collection that have descendant matching the selector.
	 * 
	 * @param iterable Descendant selector
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q has(Iterable<T> iterable) {
		return function(new HasFunction<T>(iterable, provider));
	}

	/**
	 * Filter Resource collection to the last element.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q last() {
		return function(new LastFunction<T>());
	}

	/**
	 * Return the next sibling for each Resource in the collection.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q next() {
		return function(new NextFunction<T>(provider));
	}

	/**
	 * Return the next sibling for each Resource in the collection and filter it by a selector. If the next
	 * sibling doesn't match it, empty collection will be returned.
	 * 
	 * @param selector Next sibling filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q next(String selector) {
		return function(new NextFunction<T>(provider), selector);
	}

	/**
	 * Return the next sibling for each Resource in the collection and filter it by a selector. If the next
	 * sibling doesn't match it, empty collection will be returned.
	 * 
	 * @param predicate Next sibling filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q next(Predicate<T> predicate) {
		return function(new NextFunction<T>(provider), predicate);
	}

	/**
	 * Return the next sibling for each Resource in the collection and filter it by a selector. If the next
	 * sibling doesn't match it, empty collection will be returned.
	 * 
	 * @param iterable Next sibling filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q next(Iterable<T> iterable) {
		return function(new NextFunction<T>(provider), iterable);
	}

	/**
	 * Return all following siblings for each Resource in the collection.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q nextAll() {
		return function(new NextFunction<T>(new RejectingPredicate<T>(), provider));
	}

	/**
	 * Return all following siblings for each Resource in the collection, filtering them by a selector.
	 * 
	 * @param selector Following siblings filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q nextAll(String selector) {
		return function(new NextFunction<T>(new RejectingPredicate<T>(), provider), selector);
	}

	/**
	 * Return all following siblings for each Resource in the collection, filtering them by a selector.
	 * 
	 * @param predicate Following siblings filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q nextAll(Predicate<T> predicate) {
		return function(new NextFunction<T>(new RejectingPredicate<T>(), provider), predicate);
	}

	/**
	 * Return all following siblings for each Resource in the collection, filtering them by a selector.
	 * 
	 * @param iterable Following siblings filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q nextAll(Iterable<T> iterable) {
		return function(new NextFunction<T>(new RejectingPredicate<T>(), provider), iterable);
	}

	/**
	 * Return all following siblings for each Resource in the collection up to, but not including, Resource
	 * matched by a selector.
	 * 
	 * @param until Selector marking when the operation should stop
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q nextUntil(String until) {
		return function(new NextFunction<T>(parse(until), provider));
	}

	/**
	 * Return all following siblings for each Resource in the collection up to, but not including, Resource
	 * matched by a selector.
	 * 
	 * @param predicate Selector marking when the operation should stop
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q nextUntil(Predicate<T> predicate) {
		return function(new NextFunction<T>(predicate, provider));
	}

	/**
	 * Return all following siblings for each Resource in the collection up to, but not including, Resource
	 * matched by a selector.
	 * 
	 * @param iterable Selector marking when the operation should stop
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q nextUntil(Iterable<T> iterable) {
		return nextUntil(new IterableContainsPredicate<T>(iterable, provider));
	}

	/**
	 * Remove elements from the collection.
	 * 
	 * @param selector Selector used to remove Resources
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q not(String selector) {
		return function(new NotFunction<T>(parse(selector)));
	}

	/**
	 * Remove elements from the collection.
	 * 
	 * @param predicate Selector used to remove Resources
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q not(Predicate<T> predicate) {
		return function(new FilterFunction<T>(new RejectingPredicate<T>(predicate)));
	}

	/**
	 * Remove elements from the collection.
	 * 
	 * @param iterable Selector used to remove Resources
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q not(Iterable<T> iterable) {
		return not(new IterableContainsPredicate<T>(iterable, provider));
	}

	/**
	 * Replace each element in the collection with its parent.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q parent() {
		return function(new ParentFunction<T>(provider));
	}

	/**
	 * For each element in the collection find its all ancestor.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q parents() {
		return function(new ParentsFunction<T>(new RejectingPredicate<T>(), provider));
	}

	/**
	 * For each element in the collection find its all ancestor, filtered by a selector.
	 * 
	 * @param selector Parents filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q parents(String selector) {
		return function(new ParentsFunction<T>(new RejectingPredicate<T>(), provider), selector);
	}

	/**
	 * For each element in the collection find its all ancestor, filtered by a selector.
	 * 
	 * @param predicate Parents filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q parents(Predicate<T> predicate) {
		return function(new ParentsFunction<T>(new RejectingPredicate<T>(), provider), predicate);
	}

	/**
	 * For each element in the collection find its all ancestor, filtered by a selector.
	 * 
	 * @param predicate Parents filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q parents(Iterable<T> iterable) {
		return function(new ParentsFunction<T>(new RejectingPredicate<T>(), provider), iterable);
	}

	/**
	 * For each element in the collection find all of its ancestors until the predicate is met.
	 * 
	 * @param until Selector marking when the operation should stop
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q parentsUntil(String until) {
		return function(new ParentsFunction<T>(parse(until), provider));
	}

	/**
	 * For each element in the collection find all of its ancestors until the predicate is met.
	 * 
	 * @param predicate Selector marking when the operation should stop
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q parentsUntil(Predicate<T> predicate) {
		return function(new ParentsFunction<T>(predicate, provider));
	}

	/**
	 * For each element in the collection find all of its ancestors until the predicate is met.
	 * 
	 * @param iterable Selector marking when the operation should stop
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q parentsUntil(Iterable<T> iterable) {
		return parentsUntil(new IterableContainsPredicate<T>(iterable, provider));
	}

	/**
	 * Return the previous sibling for each Resource in the collection.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q prev() {
		return function(new PrevFunction<T>(provider));
	}

	/**
	 * Return the previous sibling for each Resource in the collection and filter it by a selector. If the
	 * previous sibling doesn't match it, empty collection will be returned.
	 * 
	 * @param selector Previous sibling filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q prev(String selector) {
		return function(new PrevFunction<T>(null, provider), selector);
	}

	/**
	 * Return the previous sibling for each Resource in the collection and filter it by a selector. If the
	 * previous sibling doesn't match it, empty collection will be returned.
	 * 
	 * @param predicate Previous sibling filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q prev(Predicate<T> predicate) {
		return function(new PrevFunction<T>(null, provider), predicate);
	}

	/**
	 * Return the previous sibling for each Resource in the collection and filter it by a selector. If the
	 * previous sibling doesn't match it, empty collection will be returned.
	 * 
	 * @param iterable Previous sibling filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q prev(Iterable<T> iterable) {
		return function(new PrevFunction<T>(null, provider), iterable);
	}

	/**
	 * Return all previous siblings for each Resource in the collection.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q prevAll() {
		return function(new PrevFunction<T>(new RejectingPredicate<T>(), provider));
	}

	/**
	 * Return all previous siblings for each Resource in the collection, filtering them by a selector.
	 * 
	 * @param selector Previous siblings filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q prevAll(String selector) {
		return function(new PrevFunction<T>(new RejectingPredicate<T>(), provider), selector);
	}

	/**
	 * Return all previous siblings for each Resource in the collection, filtering them by a selector.
	 * 
	 * @param predicate Previous siblings filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q prevAll(Predicate<T> predicate) {
		return function(new PrevFunction<T>(new RejectingPredicate<T>(), provider), predicate);
	}

	/**
	 * Return all previous siblings for each Resource in the collection, filtering them by a selector.
	 * 
	 * @param iterable Previous siblings filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q prevAll(Iterable<T> iterable) {
		return function(new PrevFunction<T>(new RejectingPredicate<T>(), provider), iterable);
	}

	/**
	 * Return all previous siblings for each Resource in the collection up to, but not including, Resource
	 * matched by a selector.
	 * 
	 * @param until Selector marking when the operation should stop
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q prevUntil(String until) {
		return function(new PrevFunction<T>(parse(until), provider));
	}

	/**
	 * Return all previous siblings for each Resource in the collection up to, but not including, Resource
	 * matched by a selector.
	 * 
	 * @param predicate Selector marking when the operation should stop
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q prevUntil(Predicate<T> predicate) {
		return function(new PrevFunction<T>(predicate, provider));
	}

	/**
	 * Return all previous siblings for each Resource in the collection up to, but not including, Resource
	 * matched by a selector.
	 * 
	 * @param iterable Selector marking when the operation should stop
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q prevUntil(Iterable<T> iterable) {
		return prevUntil(new IterableContainsPredicate<T>(iterable, provider));
	}

	/**
	 * Set new search strategy, which will be used in {@link Q#find()} and {@link Q#has(String)} functions.
	 * 
	 * @param strategy Search strategy type
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q searchStrategy(SearchStrategy strategy) {
		return clone(this, strategy);
	}

	/**
	 * Return siblings for the given Ts.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q siblings() {
		return siblings("");
	}

	/**
	 * Return siblings for the given Resources filtered by a selector.
	 * 
	 * @param selector Siblings filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q siblings(String selector) {
		return function(new SiblingsFunction<T>(provider), selector);
	}

	/**
	 * Return siblings for the given Resources filtered by a selector.
	 * 
	 * @param predicate Siblings filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q siblings(Predicate<T> predicate) {
		return function(new SiblingsFunction<T>(provider), predicate);
	}

	/**
	 * Return siblings for the given Resources filtered by a selector.
	 * 
	 * @param iterable Siblings filter
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q siblings(Iterable<T> iterable) {
		return function(new SiblingsFunction<T>(provider), iterable);
	}

	/**
	 * Filter out first {@code from} Resources from the collection.
	 * 
	 * @param from How many Resources to cut out
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q slice(int from) {
		if (from < 0) {
			throw new IndexOutOfBoundsException();
		}
		return function(new SliceFunction<T>(from));
	}

	/**
	 * Reduce the collection to a subcollection specified by a given range. Both from and to are inclusive,
	 * 0-based indices.
	 * 
	 * @param from Low endpoint (inclusive) of the subcollection
	 * @param to High endpoint (inclusive) of the subcollection
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q slice(int from, int to) {
		if (from < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (from > to) {
			throw new IllegalArgumentException();
		}
		return function(new SliceFunction<T>(from, to));
	}

	/**
	 * Filter out repeated adjacent resources.
	 * 
	 * @return new SlingQuery object transformed by this operation
	 */
	public Q unique() {
		return function(new UniqueFunction<T>(provider));
	}

	private Q function(Function<?, ?> function, Iterable<T> iterable) {
		Q newQuery = clone(this, this.searchStrategy);
		newQuery.functions.add(function);
		newQuery.functions.add(new FilterFunction<T>(new IterableContainsPredicate<T>(iterable, provider)));
		return newQuery;
	}

	private Q function(Function<?, ?> function, Predicate<T> predicate) {
		Q newQuery = clone(this, this.searchStrategy);
		newQuery.functions.add(function);
		newQuery.functions.add(new FilterFunction<T>(predicate));
		return newQuery;
	}

	private Q function(Function<?, ?> function, String selector) {
		Q newQuery = clone(this, this.searchStrategy);
		newQuery.functions.add(function);
		newQuery.functions.add(new SelectorFunction<T>(selector, provider, searchStrategy));
		return newQuery;
	}

	private Q function(Function<?, ?> function) {
		Q newQuery = clone(this, this.searchStrategy);
		newQuery.functions.add(function);
		return newQuery;
	}

	private SelectorFunction<T> parse(String selector) {
		return new SelectorFunction<T>(selector, provider, searchStrategy);
	}

	protected abstract Q clone(AbstractQuery<T, Q> original, SearchStrategy strategy);

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("$(");
		Iterator<T> iterator = this.iterator();
		while (iterator.hasNext()) {
			builder.append('[');
			builder.append(iterator.next());
			builder.append(']');
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		builder.append(")");
		return builder.toString();
	}
}