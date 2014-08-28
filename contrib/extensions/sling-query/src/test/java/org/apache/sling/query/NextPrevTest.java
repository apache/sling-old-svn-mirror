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

public class NextPrevTest {

	private static final String PAR_PATH = "home/java/labels/jcr:content/par";

	private Resource tree = TestUtils.getTree();

	@Test
	public void testNext() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue")).next();
		assertResourceSetEquals(query.iterator(), "configvalue_0");
	}

	@Test
	public void testPrev() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue")).prev();
		assertResourceSetEquals(query.iterator(), "richtext");
	}

	@Test
	public void testNextFiltered() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue")).next(
				"demo/core/components/configValue");
		assertResourceSetEquals(query.iterator(), "configvalue_0");
	}

	@Test
	public void testPrevFiltered() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue")).prev(
				"demo/core/components/richtext");
		assertResourceSetEquals(query.iterator(), "richtext");
	}

	@Test
	public void testNextInvalidFiltered() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue")).next("cq:Undefined");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testPrevInvalidFiltered() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue")).prev("cq:Undefined");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testNextOnLast() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue_2")).next();
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testPrevOnFirst() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("richtext")).prev();
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testNextOnRoot() {
		SlingQuery query = $(tree).next();
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testPrevOnRoot() {
		SlingQuery query = $(tree).prev();
		assertEmptyIterator(query.iterator());
	}
}