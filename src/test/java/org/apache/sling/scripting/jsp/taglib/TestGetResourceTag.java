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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit Tests for the Class GetResourceTag.
 * 
 * @author dklco
 * @see org.apache.sling.scripting.jsp.taglib.GetResourceTag
 */
public class TestGetResourceTag {

	private static final Logger log = LoggerFactory
			.getLogger(TestGetResourceTag.class);
	private GetResourceTag getResourceTag;
	private MockResource resource;
	private MockPageContext pageContext;
	private static final String VAR_KEY = "resource";
	private static final String TEST_PATH = "/content";
	private static final String TEST_NON_PATH = "/content/page";

	/**
	 * Initializes the fields for this test.
	 */
	@SuppressWarnings("serial")
	@Before
	public void init() {
		log.info("init");

		final MockResourceResolver resolver = new MockResourceResolver();
		resource = new MockResource(resolver, TEST_PATH, "test");
		resolver.addResource(resource);

		getResourceTag = new GetResourceTag() {
			protected ResourceResolver getResourceResolver() {
				return resolver;
			}
		};

		pageContext = new MockPageContext();
		getResourceTag.setPageContext(pageContext);

		log.info("init Complete");
	}

	/**
	 * Tests using a 'good' path.
	 */
	@Test
	public void testGoodPath() {
		log.info("testGoodPath");

		getResourceTag.setVar(VAR_KEY);
		getResourceTag.setPath(TEST_PATH);
		getResourceTag.doEndTag();
		Object result = pageContext.getAttribute(VAR_KEY);
		assertNotNull(result);
		assertTrue(result instanceof Resource);
		assertEquals(TEST_PATH, ((Resource) result).getPath());

		log.info("Test successful!");
	}

	/**
	 * Tests to see what happens if a bad path is specified, this should just
	 * return a null value instead of a resource.
	 */
	@Test
	public void testBadPath() {
		log.info("testBadPath");

		getResourceTag.setVar(VAR_KEY);
		getResourceTag.setPath(TEST_NON_PATH);
		getResourceTag.doEndTag();
		Object result = pageContext.getAttribute(VAR_KEY);
		assertNull(result);

		log.info("Test successful!");
	}
}
