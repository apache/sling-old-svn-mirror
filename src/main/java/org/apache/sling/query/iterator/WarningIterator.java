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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WarningIterator<T> extends AbstractIterator<T> {

	private static final Logger LOG = LoggerFactory.getLogger("SlingQuery");

	private static final int DEFAULT_LIMIT = 100;

	private final Iterator<T> iterator;

	private final int limit;

	private int count = 0;

	public WarningIterator(Iterator<T> iterator) {
		this(iterator, DEFAULT_LIMIT);
	}

	public WarningIterator(Iterator<T> iterator, int limit) {
		this.iterator = iterator;
		this.limit = limit;
	}

	@Override
	protected T getElement() {
		if (!iterator.hasNext()) {
			return null;
		}
		if (count++ == limit) {
			LOG.warn(
					"Number of processed resources exceeded {}. Consider using a JCR query instead of SlingQuery. More info here: http://git.io/h2HeUQ",
					new Object[] { limit });
		}
		return iterator.next();
	}

}
