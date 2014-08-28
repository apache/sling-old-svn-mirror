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

import static org.apache.sling.query.SlingQuery.$;
import static org.apache.sling.query.TestUtils.assertEmptyIterator;
import static org.apache.sling.query.TestUtils.assertResourceSetEquals;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.query.api.Predicate;
import org.apache.sling.query.api.SearchStrategy;
import org.junit.Test;

public class FilterTest {

	private Resource tree = TestUtils.getTree();

	@Test
	public void testFilter() {
		SlingQuery query = $(tree).searchStrategy(SearchStrategy.DFS).find()
				.filter(new Predicate<Resource>() {
					@Override
					public boolean accepts(Resource resource) {
						return "configParsys".equals(resource.getName());
					}
				});
		assertResourceSetEquals(query.iterator(), "configParsys");
	}

	@Test
	public void testFilterOnEmptyCollection() {
		SlingQuery query = $(tree).children("cq:Undefined").filter(new Predicate<Resource>() {
			@Override
			public boolean accepts(Resource resource) {
				return true;
			}
		});
		assertEmptyIterator(query.iterator());
	}
}
