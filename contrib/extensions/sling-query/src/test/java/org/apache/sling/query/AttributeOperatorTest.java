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

public class AttributeOperatorTest {

	private Resource tree = TestUtils.getTree();

	@Test
	public void testEquals() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title=CQ Commons demo]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testEqualsWithMultivalue() {
		SlingQuery query = $(tree).children("cq:PageContent[cq:allowedTemplates=other demo template]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testNotEquals() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title=123]");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testContains() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title*=mmons de]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testContainsWithMultivalue() {
		SlingQuery query = $(tree).children("cq:PageContent[cq:allowedTemplates*=her demo templa]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testNotContains() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title*=123]");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testContainsWord() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title~=Commons]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testContainsWordWithMultivalue() {
		SlingQuery query = $(tree).children("cq:PageContent[cq:allowedTemplates~=template]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testNotContainsWord() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title~=mmons de]");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testEndsWith() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title$=demo]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testEndsWithWithMultivalue() {
		SlingQuery query = $(tree).children("cq:PageContent[cq:allowedTemplates$=demo template]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testNotEndsWith() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title$=CQ]");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testNotEquals2() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title!=123]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testNotNotEquals() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title!=CQ Commons demo]");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testStartsWith() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title^=CQ]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testStartsWithWithMultivalue() {
		SlingQuery query = $(tree).children("cq:PageContent[cq:allowedTemplates^=other demo]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testNotStartsWith() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title^=Commons]");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testHas() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testNotHas() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title123]");
		assertEmptyIterator(query.iterator());
	}

	@Test
	public void testMultipleAttributes() {
		SlingQuery query = $(tree).children("cq:PageContent[jcr:title=CQ Commons demo][jcr:createdBy=admin]");
		assertResourceSetEquals(query.iterator(), "jcr:content");
	}

	@Test
	public void testNotMultipleAttributes() {
		SlingQuery query = $(tree).children(
				"cq:PageContent[jcr:title=CQ Commons demo aaa][jcr:createdBy=admin]");
		assertEmptyIterator(query.iterator());
	}
}
