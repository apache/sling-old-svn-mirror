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
import static org.apache.sling.query.TestUtils.assertResourceSetEquals;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.query.api.SearchStrategy;
import org.junit.Test;

public class SelectorFunctionTest {

	// children with indexes:
	// 0 - richtext
	// 1 - configvalue
	// 2 - configvalue_0
	// 3 - configvalue_1
	// 4 - configvalue_2
	private static final String PAR_PATH = "home/java/labels/jcr:content/par";

	private Resource tree = TestUtils.getTree();

	@Test
	public void testEq() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children(":eq(2)");
		assertResourceSetEquals(query.iterator(), "configvalue_0");
	}

	@Test
	public void testFirst() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children(":first");
		assertResourceSetEquals(query.iterator(), "richtext");
	}

	@Test
	public void testLast() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children(":last");
		assertResourceSetEquals(query.iterator(), "configvalue_2");
	}

	@Test
	public void testGt() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children(":gt(2)");
		assertResourceSetEquals(query.iterator(), "configvalue_1", "configvalue_2");
	}

	@Test
	public void testLt() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children(":lt(3)");
		assertResourceSetEquals(query.iterator(), "richtext", "configvalue", "configvalue_0");
	}

	@Test
	public void testHas() {
		SlingQuery query = $(tree.getChild("home/java")).searchStrategy(SearchStrategy.DFS).children(
				":has([key=helloWorld])");
		assertResourceSetEquals(query.iterator(), "labels");
	}

	@Test
	public void testParent() {
		SlingQuery query = $(tree.getChild("home/java/email/jcr:content/par")).children(":parent");
		assertResourceSetEquals(query.iterator(), "email");
	}

	@Test
	public void testOdd() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children(":odd");
		assertResourceSetEquals(query.iterator(), "configvalue", "configvalue_1");
	}

	@Test
	public void testEven() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children(":even");
		assertResourceSetEquals(query.iterator(), "richtext", "configvalue_0", "configvalue_2");
	}

	@Test
	public void testSimpleNot() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children(":not(demo/core/components/richtext)");
		assertResourceSetEquals(query.iterator(), "configvalue", "configvalue_0", "configvalue_1",
				"configvalue_2");
	}

	@Test
	public void testNotFirst() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children(":not(:first)");
		assertResourceSetEquals(query.iterator(), "configvalue", "configvalue_0", "configvalue_1",
				"configvalue_2");
	}

	@Test
	public void testNotLast() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children(":not(:last)");
		assertResourceSetEquals(query.iterator(), "richtext", "configvalue", "configvalue_0", "configvalue_1");
	}

	@Test
	public void testComplexNot() {
		SlingQuery query = $(tree.getChild(PAR_PATH)).children(":not(:first):not(:last)");
		assertResourceSetEquals(query.iterator(), "configvalue", "configvalue_0", "configvalue_1");
	}
}