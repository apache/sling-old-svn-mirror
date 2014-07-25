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

public class NextPrevAllTest {

	private static final String PAR_PATH = "home/java/labels/jcr:content/par";

	private Resource tree = TestUtils.getTree();

	@Test
	public void testNextAll() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue_0")).nextAll();
		assertResourceSetEquals(query.iterator(), "configvalue_1", "configvalue_2");
	}

	@Test
	public void testPrevAll() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue_0")).prevAll();
		assertResourceSetEquals(query.iterator(), "richtext", "configvalue");
	}

	@Test
	public void testNextAllFiltered() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue")).nextAll(
				"demo/core/components/configValue");
		assertResourceSetEquals(query.iterator(), "configvalue_0", "configvalue_1", "configvalue_2");
	}

	@Test
	public void testPrevAllFiltered() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue_0")).prevAll(
				"demo/core/components/richtext");
		assertResourceSetEquals(query.iterator(), "richtext");
	}

	@Test
	public void testNextAllInvalidFiltered() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue")).nextAll("cq:Undefined");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testPrevAllInvalidFiltered() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue")).prevAll("cq:Undefined");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testNextAllOnLast() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue_2")).nextAll();
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testPrevAllOnFirst() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("richtext")).prevAll();
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testNextAllOnRoot() {
		SlingQuery query = $(tree).nextAll();
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testPrevAllOnRoot() {
		SlingQuery query = $(tree).prevAll();
		assertEmptyIterator(query.iterator());
	}
}