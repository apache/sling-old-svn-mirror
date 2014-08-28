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

import java.util.List;
import java.util.ListIterator;

import org.apache.sling.api.resource.Resource;
import org.junit.Assert;
import org.junit.Test;

public class ListTest {

	private Resource tree = TestUtils.getTree();

	@Test
	public void testIterator() {
		List<Resource> list = $(tree).children().asList();
		assertResourceSetEquals(list.iterator(), "jcr:content", "application", "home");
	}
	
	@Test
	public void testListIterator() {
		ListIterator<Resource> iterator = $(tree).children().asList().listIterator();
		Assert.assertEquals("jcr:content", iterator.next().getName());
		Assert.assertEquals("application", iterator.next().getName());
		Assert.assertEquals("home", iterator.next().getName());
		Assert.assertEquals("home", iterator.previous().getName());
		Assert.assertEquals("application", iterator.previous().getName());
		Assert.assertEquals("jcr:content", iterator.previous().getName());
	}

	@Test
	public void testSize() {
		List<Resource> list = $(tree).children().asList();
		Assert.assertEquals(3, list.size());
	}

	@Test
	public void testGet() {
		List<Resource> list = $(tree).children().asList();
		Assert.assertEquals("jcr:content", list.get(0).getName());
		Assert.assertEquals("application", list.get(1).getName());
		Assert.assertEquals("home", list.get(2).getName());
	}

	@Test
	public void testIndexOf() {
		Resource home = $(tree).children("#home").iterator().next();
		List<Resource> list = $(tree).children().asList();
		Assert.assertEquals(2, list.indexOf(home));
		Assert.assertEquals(-1, list.indexOf(tree));
	}

	@Test
	public void testContains() {
		Resource home = $(tree).children("#home").iterator().next();
		List<Resource> list = $(tree).children().asList();
		Assert.assertEquals(true, list.contains(home));
		Assert.assertEquals(false, list.contains(tree));
	}

	@Test
	public void testEmpty() {
		Assert.assertEquals(true, $(tree).children("#aaa").asList().isEmpty());
		Assert.assertEquals(false, $(tree).children().asList().isEmpty());
	}

	@Test
	public void testSublist() {
		List<Resource> list = $(tree).children().asList().subList(1, 3);
		assertResourceSetEquals(list.iterator(), "application", "home");
	}
}
