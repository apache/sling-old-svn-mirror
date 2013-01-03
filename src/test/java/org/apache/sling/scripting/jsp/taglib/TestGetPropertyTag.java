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

import java.util.HashMap;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit Tests for the Class GetPropertyTag.
 * @see org.apache.sling.scripting.jsp.taglib.GetPropertyTag
 */
public class TestGetPropertyTag {

	private static final Logger log = LoggerFactory
			.getLogger(TestGetPropertyTag.class);
	private GetPropertyTag getPropertyTag;
	private MockPageContext pageContext;
	private static final String VAR_KEY = "value";
	private static final String TEST_KEY = "key";
	private static final String TEST_EMPTY_KEY = "key2";
	private static final String TEST_VALUE = "Sling";

	/**
	 * Initializes the fields for this test.
	 */
	@SuppressWarnings("serial")
	@Before
	public void init() {
		log.info("init");
		getPropertyTag = new GetPropertyTag() {
			protected ClassLoader getClassLoader() {
				return TestGetPropertyTag.class.getClassLoader();
			}
		};

		pageContext = new MockPageContext();
		getPropertyTag.setPageContext(pageContext);
	}

	/**
	 * Tests the adapt object Tag functionality.
	 */
	@Test
	public void testAdaptObject() {
		log.info("testAdaptObject");

		log.info("Setting up tests");
		ValueMap properties = new ValueMapDecorator(
				new HashMap<String, Object>());
		properties.put(TEST_KEY, TEST_VALUE);
		getPropertyTag.setProperties(properties);
		getPropertyTag.setVar(VAR_KEY);

		log.info("Testing retrieving value with default value");
		getPropertyTag.setDefaultValue(TEST_VALUE);
		getPropertyTag.setKey(TEST_KEY);
		getPropertyTag.doEndTag();
		Object value = pageContext.getAttribute(VAR_KEY);
		assertNotNull(value);
		assertEquals(TEST_VALUE, value);
		
		log.info("Testing retrieving default value");
		getPropertyTag.setDefaultValue(TEST_VALUE);
		getPropertyTag.setKey(TEST_EMPTY_KEY);
		getPropertyTag.doEndTag();
		value = pageContext.getAttribute(VAR_KEY);
		assertNotNull(value);
		assertEquals(TEST_VALUE, value);

		log.info("Testing retrieving value");
		getPropertyTag.setDefaultValue(null);
		getPropertyTag.setReturnClass(String.class.getCanonicalName());
		getPropertyTag.setKey(TEST_KEY);
		getPropertyTag.doEndTag();
		value = pageContext.getAttribute(VAR_KEY);
		assertNotNull(value);
		assertEquals(TEST_VALUE, value);

		log.info("Test successful!");
	}
}
