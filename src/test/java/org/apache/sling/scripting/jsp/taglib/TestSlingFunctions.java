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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit Tests for the Class SlingFunctions.
 * 
 * @see org.apache.sling.scripting.jsp.taglib.SlingFunctions
 */
public class TestSlingFunctions {

	private static final Logger log = LoggerFactory
			.getLogger(TestGetResourceTag.class);
	private MockResource resource;
	private MockResourceResolver resolver;
	private Date date;
	private static final String TEST_PATH = "/content";

	/**
	 * Initializes the fields for this test.
	 */
	@Before
	public void init() {
		log.info("init");

		resolver = new MockResourceResolver() {
			@Override
			public Iterator<Resource> findResources(String query,
					String language) {
				if (query.equals("query") && language.equals("language")) {
					List<Resource> resources = new ArrayList<Resource>();
					resources.add(resource);
					return resources.iterator();
				} else {
					return null;
				}
			}
		};
		resource = new MockResource(resolver, TEST_PATH, "test");
		this.date = new Date();
		resource.addProperty("date", date);
		resource.addProperty("long", new Long(0L));
		resolver.addResource(resource);
		MockResource child1 = new MockResource(resolver, TEST_PATH + "/child1",
				"test");
		resolver.addResource(child1);
		MockResource child2 = new MockResource(resolver, TEST_PATH + "/child2",
				"test");
		resolver.addResource(child2);

		log.info("init Complete");
	}

	@Test
	public void testEscape() {
		log.info("testEncode");

		log.info("Testing HTML Escaping");
		assertEquals("&amp;nbsp&#x3b;Here is some text&#x21;",
				SlingFunctions.escape("&nbsp;Here is some text!", "HTML"));

		log.info("Testing HTML Attr Escaping");
		assertEquals(
				"&amp;nbsp&#x3b;Here&#x20;is&#x20;some&#x20;text&#x21;&quot;",
				SlingFunctions
						.escape("&nbsp;Here is some text!\"", "HTML_ATTR"));

		log.info("Testing invalid values");
		try {
			SlingFunctions.escape(null, null);
			fail("Expected null pointer exception");
		} catch (NullPointerException npe) {
			log.info("Encountered expected exception");
		}
		try {
			SlingFunctions.escape(null, "Invalid");
			fail("Expected invalid argument exception");
		} catch (IllegalArgumentException iae) {
			log.info("Encountered expected exception");
		}

		log.info("Testing null/empty values");
		assertNull(SlingFunctions.escape(null, "html"));
		assertEquals("", SlingFunctions.escape("", "html"));

		log.info("Tests successful!");
	}

	@Test
	public void testGetResource() {
		log.info("testGetResource");
		Resource resource = SlingFunctions.getResource(resolver, TEST_PATH);
		assertNotNull(resource);
		assertEquals(TEST_PATH, resource.getPath());

		log.info("Tests successful!");
	}

	@Test
	public void testFindResources() throws ClassNotFoundException {
		log.info("testFindResources");

		Iterator<Resource> resources = SlingFunctions.findResources(resolver,
				"query", "language");
		assertNotNull(resources);
		assertTrue(resources.hasNext());
		assertEquals(resource, resources.next());

		log.info("Tests successful!");
	}

	@Test
	public void testAdaptTo() throws ClassNotFoundException {
		log.info("testAdaptTo");

		Adaptable adaptable = SlingFunctions.getResource(resolver, TEST_PATH);
		Object adapted = SlingFunctions.adaptTo(adaptable,
				ValueMap.class.getCanonicalName());
		assertNotNull(adapted);
		assertTrue(adapted instanceof ValueMap);

		log.info("Tests successful!");
	}

	@Test
	public void testGetRelativeResource() {
		log.info("testGetRelativeResource");
		Resource parent = SlingFunctions.getResource(resolver, TEST_PATH);
		Resource child = SlingFunctions.getRelativeResource(parent, "child1");
		assertNotNull(child);
		assertEquals(TEST_PATH + "/child1", child.getPath());

		log.info("Tests successful!");
	}

	@Test
	public void testGetValue() {
		log.info("testGetValue");
		Resource resource = SlingFunctions.getResource(resolver, TEST_PATH);
		ValueMap properties = resource.adaptTo(ValueMap.class);

		log.info("Testing using class coersion");
		Date retrievedDate = SlingFunctions.getValue(properties, "date",
				Date.class);
		assertEquals(date, retrievedDate);
		assertTrue(retrievedDate instanceof Date);

		log.info("Testing with default value on existing key");
		Long retrievedLong = SlingFunctions.getValue(properties, "long",
				new Long(-123L));
		assertEquals(new Long(0L), retrievedLong);
		assertTrue(retrievedLong instanceof Long);

		log.info("Testing with no value and class coersion");
		Date fakeDate = SlingFunctions
				.getValue(properties, "date1", Date.class);
		assertTrue(fakeDate == null);

		log.info("Testing with no value and default specified");
		Long fakeLong = SlingFunctions.getValue(properties, "long1", new Long(
				-123L));
		assertEquals(new Long(-123L), fakeLong);
		assertTrue(fakeLong instanceof Long);

		log.info("Tests successful!");
	}

	@Test
	public void testListChildResources() {
		log.info("testListChildResources");
		Iterator<Resource> children = SlingFunctions.listChildren(resource);
		assertNotNull(children);
		assertTrue(children.hasNext());

		log.info("Tests successful!");
	}
}
