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

public class MergingIterator<T> extends AbstractIterator<T> {

	private final Iterator<T>[] iterators;

	private int index = 0;

	public MergingIterator(Iterator<T>... iterators) {
		this.iterators = iterators;
	}

	@Override
	protected T getElement() {
		while (index < iterators.length) {
			if (iterators[index].hasNext()) {
				return iterators[index].next();
			} else {
				index++;
			}
		}
		return null;
	}

}
