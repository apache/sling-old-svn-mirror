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

import org.apache.sling.query.api.Function;
import org.apache.sling.query.api.internal.ElementToIteratorFunction;
import org.apache.sling.query.api.internal.IteratorToIteratorFunction;
import org.apache.sling.query.api.internal.Option;
import org.apache.sling.query.iterator.ExpandingIterator;

public class IteratorToIteratorFunctionWrapper<T> implements IteratorToIteratorFunction<T> {

	private final Function<?, ?> function;

	public IteratorToIteratorFunctionWrapper(Function<?, ?> function) {
		this.function = function;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<Option<T>> apply(Iterator<Option<T>> parentIterator) {
		if (function instanceof ElementToIteratorFunction) {
			return getOptionIterator((ElementToIteratorFunction<T>) function, parentIterator);
		} else if (function instanceof IteratorToIteratorFunction) {
			return ((IteratorToIteratorFunction<T>) function).apply(parentIterator);
		} else {
			throw new IllegalArgumentException("Don't know how to handle " + function.toString());
		}
	}

	private static <T> Iterator<Option<T>> getOptionIterator(ElementToIteratorFunction<T> function,
			Iterator<Option<T>> parentIterator) {
		return new ExpandingIterator<T>((ElementToIteratorFunction<T>) function, parentIterator);
	}
}
