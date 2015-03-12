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
import java.util.List;

import org.apache.sling.query.api.Function;
import org.apache.sling.query.api.internal.IteratorToIteratorFunction;
import org.apache.sling.query.api.internal.Option;

public class CompositeFunction<T> implements IteratorToIteratorFunction<T> {

	private final List<Function<?, ?>> functions;

	public CompositeFunction(List<Function<?, ?>> functions) {
		this.functions = functions;
	}

	@Override
	public Iterator<Option<T>> apply(Iterator<Option<T>> input) {
		Iterator<Option<T>> iterator = input;
		for (Function<?, ?> f : functions) {
			iterator = new IteratorToIteratorFunctionWrapper<T>(f).apply(iterator);
		}
		return iterator;
	}
}
