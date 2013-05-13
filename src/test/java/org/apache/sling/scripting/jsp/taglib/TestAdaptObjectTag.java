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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit Tests for the Class AdaptObject.
 * @see org.apache.sling.scripting.jsp.taglib.AdaptObjectTag
 */
public class TestAdaptObjectTag {

	private static final Logger log = LoggerFactory
			.getLogger(TestAdaptObjectTag.class);
	private AdaptToTag adaptToTag;
	private MockResource resource;
	private MockPageContext pageContext;
	private static final String VAR_KEY = "properties";

	/**
	 * Initializes the fields for this test.
	 */
	@SuppressWarnings("serial")
	@Before
	public void init() {
		log.info("init");
		adaptToTag = new AdaptToTag() {
			protected ClassLoader getClassLoader() {
				return TestAdaptObjectTag.class.getClassLoader();
			}
		};

		pageContext = new MockPageContext();
		adaptToTag.setPageContext(pageContext);

		ResourceResolver resolver = new MockResourceResolver();
		resource = new MockResource(resolver, "/", "test");
		log.info("init Complete");
	}

	/**
	 * Tests the adapt object Tag functionality.
	 */
	@Test
	public void testAdaptObject() {
		log.info("testAdaptObject");

		log.info("Setting up tests");
		adaptToTag.setAdaptable(resource);
		adaptToTag.setAdaptTo(ValueMap.class.getCanonicalName());
		adaptToTag.setVar(VAR_KEY);
		adaptToTag.doEndTag();

		log.info("Checking result");
		Object result = pageContext.getAttribute(VAR_KEY);
		assertNotNull(result);
		assertTrue(result instanceof ValueMap);

		log.info("Test successful!");
	}

	/**
	 * Tests to ensure that a null result is returned if the class to adapt does
	 * not exist.
	 */
	@Test
	public void testMissingClass() {
		log.info("testMissingClass");

		log.info("Setting up tests");
		adaptToTag.setAdaptable(resource);
		adaptToTag.setAdaptTo("com.bad.class");
		adaptToTag.setVar(VAR_KEY);
		adaptToTag.doEndTag();

		log.info("Checking result");
		Object result = pageContext.getAttribute(VAR_KEY);
		assertNull(result);

		log.info("Test successful!");
	}
}
