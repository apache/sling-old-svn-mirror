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
package org.apache.sling.jcr.resource.internal.helper.starresource;

import junit.framework.TestCase;

import org.apache.sling.api.resource.ResourceMetadata;

/** Test the StarResource */
public class StarResourceTest extends TestCase {
	private void assertSplit(String requestPath, String path, String pathInfo) {
		final ResourceMetadata rm = StarResource.getResourceMetadata(requestPath);
		assertEquals("For requestPath=" + requestPath + ", path matches", path, rm.getResolutionPath());
		assertEquals("For requestPath=" + requestPath + ", pathInfo matches", pathInfo, rm.getResolutionPathInfo());
	}
	
	public void testSimplePath() {
		assertSplit("/foo/*.html", "/foo/*", ".html");
	}
	
	public void testNoExtension() {
		assertSplit("/foo/*", "/foo/*", "");
	}
	
	public void testNoStar() {
		assertSplit("/foo/bar.html", "/foo/bar.html", null);
	}
	
	public void testTwoStars() {
		assertSplit("/foo/*.html/*.txt", "/foo/*", ".html/*.txt");
	}
}
