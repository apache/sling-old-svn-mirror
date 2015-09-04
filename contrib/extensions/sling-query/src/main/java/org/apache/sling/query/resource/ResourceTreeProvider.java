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

package org.apache.sling.query.resource;

import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.query.api.Predicate;
import org.apache.sling.query.api.internal.TreeProvider;
import org.apache.sling.query.resource.jcr.JcrQueryIterator;
import org.apache.sling.query.resource.jcr.JcrTypeResolver;
import org.apache.sling.query.resource.jcr.SessionJcrTypeResolver;
import org.apache.sling.query.selector.parser.Attribute;
import org.apache.sling.query.selector.parser.SelectorSegment;

public class ResourceTreeProvider implements TreeProvider<Resource> {

	private final JcrTypeResolver typeResolver;

	public ResourceTreeProvider(ResourceResolver resolver) {
		this.typeResolver = new SessionJcrTypeResolver(resolver);
	}

	@Override
	public Iterator<Resource> listChildren(Resource parent) {
		return parent.listChildren();
	}

	@Override
	public Resource getParent(Resource element) {
		return element.getParent();
	}

	@Override
	public String getName(Resource element) {
		return element.getName();
	}

	@Override
	public Predicate<Resource> getPredicate(String type, String id, List<Attribute> attributes) {
		return new ResourcePredicate(type, id, attributes, typeResolver);
	}

	@Override
	public Iterator<Resource> query(List<SelectorSegment> segments, Resource resource) {
		return new JcrQueryIterator(segments, resource, typeResolver);
	}

	@Override
	public boolean sameElement(Resource o1, Resource o2) {
		if (o1 == null && o2 == null) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		return o1.getPath().equals(o2.getPath());
	}

	@Override
	public boolean isDescendant(Resource root, Resource testedElement) {
		if (root == null || testedElement == null) {
			return false;
		}
		return testedElement.getPath().startsWith(root.getPath());
	}

}
