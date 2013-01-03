/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.jsp.taglib;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit Tests for the Class ListChildrenTag.
 * @see org.apache.sling.scripting.jsp.taglib.ListChildrenTag
 */
public class TestListChildrenTag {

	private static final Logger log = LoggerFactory
			.getLogger(TestListChildrenTag.class);
	private ListChildrenTag listChildrenTag = new ListChildrenTag();
	private MockResource resource;
	private MockPageContext pageContext;
	private static final String TEST_PATH = "/content";
	private static final String VAR_KEY = "children";

	/**
	 * Initializes the fields for this test.
	 */
	@Before
	public void init() {
		log.info("init");

		log.info("Creating Resource Structure");
		final MockResourceResolver resolver = new MockResourceResolver();
		resource = new MockResource(resolver, TEST_PATH, "test");
		resolver.addResource(resource);
		MockResource child1 = new MockResource(resolver, TEST_PATH + "/child1",
				"test");
		resolver.addResource(child1);
		MockResource child2 = new MockResource(resolver, TEST_PATH + "/child2",
				"test");
		resolver.addResource(child2);

		log.info("Adding page context");
		pageContext = new MockPageContext();
		listChildrenTag.setPageContext(pageContext);

		log.info("init Complete");
	}

	/**
	 * Tests to ensure the base ListChildren functionality works.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testListChildren() {
		log.info("testGoodPath");

		listChildrenTag.setResource(resource);
		listChildrenTag.setVar(VAR_KEY);
		listChildrenTag.doEndTag();
		Object result = pageContext.getAttribute(VAR_KEY);
		assertNotNull(result);
		assertTrue(result instanceof Iterator);
		assertTrue(((Iterator<Resource>) result).hasNext());

		log.info("Test successful!");
	}

	/**
	 * Tests to see what happens if a null Resource is provided
	 */
	@Test
	public void testNullResource() {
		log.info("testNullResource");

		listChildrenTag.setVar(VAR_KEY);
		listChildrenTag.setResource(null);
		listChildrenTag.doEndTag();
		Object result = pageContext.getAttribute(VAR_KEY);
		assertNull(result);

		log.info("Test successful!");
	}
}
