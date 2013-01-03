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
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSlingFunctions {

	private static final Logger log = LoggerFactory
			.getLogger(TestGetResourceTag.class);
	private MockResource resource;
	private MockResourceResolver resolver;
	private static final String TEST_PATH = "/content";

	/**
	 * Initializes the fields for this test.
	 */
	@Before
	public void init() {
		log.info("init");

		resolver = new MockResourceResolver();
		resource = new MockResource(resolver, TEST_PATH, "test");
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
	public void testGetResource() {
		log.info("testGetResource");
		Resource resource = SlingFunctions.getResource(resolver, TEST_PATH);
		assertNotNull(resource);
		assertEquals(TEST_PATH, resource.getPath());

		log.info("Tests successful!");
	}

	@Test
	public void testListChildResources() {
		log.info("testListChildResources");
		Iterator<Resource> children = SlingFunctions
				.listChildResources(resource);
		assertNotNull(children);
		assertTrue(children.hasNext());

		log.info("Tests successful!");
	}
}
