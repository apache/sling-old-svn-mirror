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

package org.apache.sling.query.selector.parser;

import static org.apache.sling.query.selector.parser.JcrSelectorTest.parse;

import org.junit.Assert;
import org.junit.Test;

public class JcrMultiSelectorTest {
	@Test
	public void typeHierarchy() {
		String selector = "cq:Page, cq:Type";
		String jcrQuery = "SELECT * FROM [cq:Page] AS s";
		Assert.assertEquals(jcrQuery, parse(selector, "/"));

		selector = "cq:Type, cq:Page";
		Assert.assertEquals(jcrQuery, parse(selector, "/"));
	}

	@Test
	public void incompatibleTypes() {
		final String selector = "jcr:someType, cq:Type";
		final String jcrQuery = "SELECT * FROM [nt:base] AS s";
		Assert.assertEquals(jcrQuery, parse(selector, "/"));
	}

	@Test
	public void attributes() {
		final String selector = "[x=y][y=z], [a=b][c=d]";
		final String jcrQuery = "SELECT * FROM [nt:base] AS s WHERE ((s.[x] = 'y' AND s.[y] = 'z') OR (s.[a] = 'b' AND s.[c] = 'd'))";
		Assert.assertEquals(jcrQuery, parse(selector, "/"));
	}

	@Test
	public void attributesWithPath() {
		final String selector = "[x=y][y=z], [a=b][c=d]";
		final String jcrQuery = "SELECT * FROM [nt:base] AS s WHERE (ISDESCENDANTNODE('/content') AND ((s.[x] = 'y' AND s.[y] = 'z') OR (s.[a] = 'b' AND s.[c] = 'd')))";
		Assert.assertEquals(jcrQuery, parse(selector, "/content"));
	}
}
