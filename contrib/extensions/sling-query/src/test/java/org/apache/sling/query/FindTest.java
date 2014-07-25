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
import static org.apache.sling.query.TestUtils.assertResourceListEquals;
import static org.apache.sling.query.TestUtils.assertResourceSetEquals;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.query.api.SearchStrategy;
import org.junit.Test;

public class FindTest {

	private Resource tree = TestUtils.getTree();

	@Test
	public void testFind() {
		SlingQuery query = $(tree.getChild("application/configuration/labels")).searchStrategy(
				SearchStrategy.DFS).find();
		assertResourceSetEquals(query.iterator(), "jcr:content", "configParsys", "tab", "tab_0", "items",
				"items", "localizedtext", "text", "text_0", "text", "lang");
	}

	@Test
	public void testFindWithFilter() {
		SlingQuery query = $(tree.getChild("application/configuration/labels")).searchStrategy(
				SearchStrategy.DFS).find("cq-commons/config/components/text");
		assertResourceSetEquals(query.iterator(), "text", "text");
	}

	@Test
	public void testFindWithResources() {
		SlingQuery query = $(tree.getChild("home")).find(
				$(tree.getChild("home/java"), tree.getChild("home/js"), tree.getChild("application")));
		assertResourceSetEquals(query.iterator(), "java", "js");
	}

	@Test
	public void testLeaveFind() {
		SlingQuery query = $(
				tree.getChild("application/configuration/labels/jcr:content/configParsys/tab/items/localizedtext/lang"))
				.searchStrategy(SearchStrategy.DFS).find();
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testEmptyFind() {
		SlingQuery query = $(tree.getChild("application/configuration/labels")).searchStrategy(
				SearchStrategy.DFS).find("cq:Undefined");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testBfsFind() {
		SlingQuery query = $(tree.getChild("application/configuration/labels")).searchStrategy(
				SearchStrategy.BFS).find("");
		assertResourceListEquals(query.iterator(), "jcr:content", "configParsys", "tab", "tab_0", "items",
				"items", "localizedtext", "text", "text_0", "text", "lang");
	}

	@Test
	public void testDfsFind() {
		SlingQuery query = $(tree.getChild("application/configuration/labels")).searchStrategy(
				SearchStrategy.DFS).find("");
		assertResourceListEquals(query.iterator(), "jcr:content", "configParsys", "tab", "items",
				"localizedtext", "lang", "text", "tab_0", "items", "text_0", "text");
	}
}
