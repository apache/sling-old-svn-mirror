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

import org.apache.sling.query.api.internal.Option;

public class AlternativeIterator<T> extends AbstractIterator<Option<T>> {

	private final List<Iterator<Option<T>>> iterators;

	public AlternativeIterator(List<Iterator<Option<T>>> iterators) {
		this.iterators = iterators;
	}

	@Override
	protected Option<T> getElement() {
		Option<T> element = null;
		for (Iterator<Option<T>> i : iterators) {
			if (i.hasNext()) {
				Option<T> option = i.next();
				if (element == null || !option.isEmpty()) {
					element = option;
				}
			}
		}
		return element;
	}
}