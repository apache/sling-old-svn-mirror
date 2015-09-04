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

import org.apache.sling.query.api.Predicate;
import org.apache.sling.query.api.internal.TreeProvider;

public class ParentsIterator<T> extends AbstractIterator<T> {

	private final Predicate<T> until;

	private final TreeProvider<T> provider;
	
	private T currentResource;

	public ParentsIterator(Predicate<T> until, T currentResource, TreeProvider<T> provider) {
		this.currentResource = currentResource;
		this.until = until;
		this.provider = provider;
	}

	@Override
	protected T getElement() {
		if (currentResource == null) {
			return null;
		}
		currentResource = provider.getParent(currentResource);

		if (currentResource == null) {
			return null;
		}

		if (until != null && until.accepts(currentResource)) {
			return null;
		}

		return currentResource;
	}

}
