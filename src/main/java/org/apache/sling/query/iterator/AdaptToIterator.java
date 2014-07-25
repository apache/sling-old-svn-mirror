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
import java.util.NoSuchElementException;

import org.apache.sling.api.adapter.Adaptable;

public class AdaptToIterator<F, T> implements Iterator<T> {

	private final Iterator<F> iterator;

	private final Class<? extends T> clazz;

	private T currentModel;

	public AdaptToIterator(Iterator<F> iterator, Class<? extends T> clazz) {
		this.clazz = clazz;
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext() {
		if (currentModel == null) {
			getCurrentModel();
		}
		return currentModel != null;
	}

	@Override
	public T next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		T model = currentModel;
		currentModel = null;
		return model;
	}

	public void getCurrentModel() {
		while (iterator.hasNext()) {
			F element = iterator.next();
			if (element instanceof Adaptable) {
				currentModel = ((Adaptable) element).adaptTo(clazz);
			}
			if (currentModel != null) {
				break;
			}
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
