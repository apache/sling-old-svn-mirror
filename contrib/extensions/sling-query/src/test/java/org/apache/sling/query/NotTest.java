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

import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.junit.Test;

public class NotTest {

	private Resource tree = TestUtils.getTree();

	@Test
	public void testNot() {
		SlingQuery query = $(tree).children().not("cq:Page");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testNotResources() {
		SlingQuery query = $(tree).children().not(
				$(tree.getChild("jcr:content"), tree.getChild("application")));
		assertResourceSetEquals(query.iterator(), "home");
	}

	@Test
	public void testNotEmptyResources() {
		SlingQuery query = $(tree).children().not(Collections.<Resource>emptyList());
		assertResourceSetEquals(query.iterator(), "jcr:content", "application", "home");
	}

	@Test
	public void testNotWithModifier() {
		SlingQuery query = $(tree).children("cq:Page").not(":first");
		assertResourceSetEquals(query.iterator(), "home");
	}

	@Test
	public void testNotOnEmptyCollection() {
		SlingQuery query = $(tree).children("cq:Undefined").not("cq:Page");
		assertEmptyIterator(query.iterator());
	}
}
