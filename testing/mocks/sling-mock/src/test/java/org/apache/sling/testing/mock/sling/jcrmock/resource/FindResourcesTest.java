/*
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
package org.apache.sling.testing.mock.sling.jcrmock.resource;

import java.util.Collections;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class FindResourcesTest {

	@Rule
	public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

	@Before
	public void setUp() {
		Resource resource = context.create().resource(
				"test",
				ImmutableMap.<String, Object> builder().put("prop1", "value1")
						.put("prop2", "value2").build());
		Node node = resource.adaptTo(Node.class);
		Session session = context.resourceResolver().adaptTo(Session.class);
		
		MockJcr.setQueryResult(session, Collections.singletonList(node));
	}

	@Test
    @SuppressWarnings("deprecation")
	public void testFindResources() {
		Resource resource = context.resourceResolver().getResource("/test");
		Assert.assertNotNull("Resource with name 'test' should be there", resource);
		
        Iterator<Resource> result = context.resourceResolver().findResources("/test", Query.XPATH);
		Assert.assertTrue("At least one result expected", result.hasNext());
		Assert.assertEquals("/test", result.next().getPath());
		Assert.assertFalse("At most one result expected", result.hasNext());
	}

}
