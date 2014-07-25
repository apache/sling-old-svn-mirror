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

public class NextPrevUntilTest {

	private static final String PAR_PATH = "home/java/labels/jcr:content/par";

	private Resource tree = TestUtils.getTree();

	@Test
	public void testNextUntil() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue")).nextUntil("[key=unknownKey]");
		assertResourceSetEquals(query.iterator(), "configvalue_0", "configvalue_1");
	}

	@Test
	public void testNextUntilResource() {
		Resource resource = tree.getChild(PAR_PATH).getChild("configvalue_2");
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue")).nextUntil($(resource));
		assertResourceSetEquals(query.iterator(), "configvalue_0", "configvalue_1");
	}

	@Test
	public void testPrevUntil() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue_2")).prevUntil("[key=helloWorld]");
		assertResourceSetEquals(query.iterator(), "configvalue_0", "configvalue_1");
	}

	@Test
	public void testPrevUntilResource() {
		Resource resource = tree.getChild(PAR_PATH).getChild("configvalue");
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue_2")).prevUntil($(resource));
		assertResourceSetEquals(query.iterator(), "configvalue_0", "configvalue_1");
	}

	@Test
	public void testNextUntilOnLast() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue_2")).nextUntil("[key=unknownKey]");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testPrevUntilOnFirst() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("richtext")).prevUntil("[key=helloWorld]");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testNextUntilOnRoot() {
		SlingQuery query = $(tree).nextUntil("cq:Page");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testPrevUntilOnRoot() {
		SlingQuery query = $(tree).prevUntil("cq:Page");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testNextUntilInvalid() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("richtext")).nextUntil("cq:Undefined");
		assertResourceSetEquals(query.iterator(), "configvalue", "configvalue_0", "configvalue_1",
				"configvalue_2");
	}

	@Test
	public void testPrevUntilInvalid() {
		SlingQuery query = $(tree.getChild(PAR_PATH).getChild("configvalue_2")).prevUntil("cq:Undefined");
		assertResourceSetEquals(query.iterator(), "configvalue", "configvalue_0", "configvalue_1", "richtext");
	}
}