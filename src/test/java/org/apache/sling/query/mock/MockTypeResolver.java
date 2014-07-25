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

package org.apache.sling.query.mock;

import java.util.Arrays;
import java.util.List;

import org.apache.sling.query.resource.jcr.JcrTypeResolver;

public class MockTypeResolver implements JcrTypeResolver {

	private static final List<String> TYPE_HIERARCHY = Arrays.asList("nt:base", "nt:unstructured", "cq:Page",
			"cq:Type");

	private static final List<String> OTHER_TYPES = Arrays.asList("jcr:otherType", "jcr:someType");

	@Override
	public boolean isJcrType(String name) {
		return TYPE_HIERARCHY.contains(name) || OTHER_TYPES.contains(name);
	}

	@Override
	public boolean isSubtype(String supertype, String subtype) {
		int i1 = TYPE_HIERARCHY.indexOf(supertype);
		int i2 = TYPE_HIERARCHY.indexOf(subtype);
		if (i1 == -1 || i2 == -1) {
			return false;
		}
		return i1 < i2;
	}

}
