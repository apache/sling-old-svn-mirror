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

import org.apache.sling.query.api.internal.IteratorToIteratorFunction;
import org.apache.sling.query.api.internal.Option;
import org.apache.sling.query.api.internal.TreeProvider;
import org.apache.sling.query.iterator.UniqueIterator;

public class UniqueFunction<T> implements IteratorToIteratorFunction<T> {

	private final TreeProvider<T> treeProvider;

	public UniqueFunction(TreeProvider<T> treeProvider) {
		this.treeProvider = treeProvider;
	}

	@Override
	public Iterator<Option<T>> apply(Iterator<Option<T>> input) {
		return new UniqueIterator<T>(input, treeProvider);
	}

}
