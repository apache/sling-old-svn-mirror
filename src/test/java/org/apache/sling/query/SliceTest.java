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
import org.junit.Test;

public class SliceTest {

	// children with indexes:
	// 0 - richtext
	// 1 - configvalue
	// 2 - configvalue_0
	// 3 - configvalue_1
	// 4 - configvalue_2
	private static final String PAR_PATH = "home/java/labels/jcr:content/par";

	private Resource tree = TestUtils.getTree();

	@Test
	public void testSlice() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children().slice(2, 4);
		assertResourceSetEquals(query.iterator(), "configvalue_0", "configvalue_1", "configvalue_2");
	}

	@Test
	public void testSliceOne() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children().slice(2, 2);
		assertResourceSetEquals(query.iterator(), "configvalue_0");
	}

	@Test
	public void testEq() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children().eq(2);
		assertResourceSetEquals(query.iterator(), "configvalue_0");
	}

	@Test
	public void testEqOnEmpty() {
		SlingQuery query = $(tree).children("cq:Undefined").eq(0);
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testFirst() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children().first();
		assertResourceSetEquals(query.iterator(), "richtext");
	}

	@Test
	public void testFirstOnEmpty() {
		SlingQuery query = $(tree).children("cq:Undefined").first();
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testSliceAll() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children().slice(0, 4);
		assertResourceSetEquals(query.iterator(), "richtext", "configvalue", "configvalue_0",
				"configvalue_1", "configvalue_2");
	}

	@Test
	public void testSliceAllBigTo() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children().slice(0, 10);
		assertResourceSetEquals(query.iterator(), "richtext", "configvalue", "configvalue_0",
				"configvalue_1", "configvalue_2");
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testSliceNegativeFrom() {
		$(tree.getChild(PAR_PATH)).children().slice(-1);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testSliceNegativeFrom2() {
		$(tree.getChild(PAR_PATH)).children().slice(-1, 2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSliceFromGreaterThanTo() {
		$(tree.getChild(PAR_PATH)).children().slice(2, 1);
	}
}