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
import java.util.List;

import org.apache.sling.query.api.internal.IteratorToIteratorFunction;
import org.apache.sling.query.api.internal.Option;

/**
 * This iterator returns all elements of the input list which are mapped to non-empty values by the input
 * function. Name is inspired by the <a href="http://en.wikipedia.org/wiki/Support_(mathematics)">support of
 * the function</a>.
 */
public class SuppIterator<T> extends AbstractIterator<Option<T>> {

	private final List<Option<T>> input;

	private final Iterator<Option<T>> output;

	private Option<T> outputElement;

	private int currentIndex = 0;

	public SuppIterator(List<Option<T>> input, IteratorToIteratorFunction<T> function) {
		this.input = input;
		this.output = function.apply(new ArgumentResettingIterator<T>(input.iterator()));
	}

	/**
	 * The idea behind this method is that index of each element in the input iterator is passed to the
	 * function. Elements returned by the output iterator contains the same index, which can be used to assign
	 * input to output elements. We check which indices are present in the output iterator and return only
	 * related input elements.
	 */
	@Override
	protected Option<T> getElement() {
		if (outputElement != null) {
			final int outputIndex = outputElement.getArgumentId();
			if (currentIndex < outputIndex) {
				return Option.empty(input.get(currentIndex++).getArgumentId());
			} else if (currentIndex == outputIndex && !outputElement.isEmpty()) {
				return input.get(currentIndex++);
			}
		}

		while (output.hasNext()) {
			outputElement = output.next();
			final int outputIndex = outputElement.getArgumentId();
			if ((outputIndex == currentIndex && !outputElement.isEmpty()) || outputIndex > currentIndex) {
				return getElement();
			}
		}
		return null;
	}
}
