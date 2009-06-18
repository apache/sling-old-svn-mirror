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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test POSTing to root node (SLING-668) */
public class PostToRootTest extends HttpTestBase {
	
	public void testSetRootProperty() throws Exception {
		String url = HTTP_BASE_URL;
		if(!url.endsWith("/")) {
			url += "/";
		}
        final PostMethod post = new PostMethod(url);
		final String name = getClass().getSimpleName();
		final String value = getClass().getSimpleName() + System.currentTimeMillis();
		post.addParameter(name, value);
		
		final int status = httpClient.executeMethod(post);
		assertEquals(200, status);
		
		final String json = getContent(url + ".json", CONTENT_TYPE_JSON);
		assertJavascript(value, json, "out.print(data." + name + ")");
	}
}