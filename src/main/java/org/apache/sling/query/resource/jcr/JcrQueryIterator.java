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

package org.apache.sling.query.resource.jcr;

import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.query.iterator.AbstractIterator;
import org.apache.sling.query.resource.jcr.query.JcrQueryBuilder;
import org.apache.sling.query.selector.parser.SelectorSegment;

public class JcrQueryIterator extends AbstractIterator<Resource> {

	private final ResourceResolver resolver;

	private final String query;

	private Iterator<Resource> currentIterator;

	public JcrQueryIterator(List<SelectorSegment> segments, Resource root, JcrTypeResolver typeResolver) {
		JcrQueryBuilder builder = new JcrQueryBuilder(typeResolver);
		query = builder.buildQuery(segments, root.getPath());
		resolver = root.getResourceResolver();
	}

	@Override
	protected Resource getElement() {
		if (currentIterator == null) {
			currentIterator = resolver.findResources(query, "JCR-SQL2");
		}
		if (currentIterator.hasNext()) {
			return currentIterator.next();
		} else {
			return null;
		}
	}
}
