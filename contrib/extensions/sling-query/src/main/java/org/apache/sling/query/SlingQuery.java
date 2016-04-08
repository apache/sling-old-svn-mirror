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

package org.apache.sling.query;

import java.util.Iterator;

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.query.api.SearchStrategy;
import org.apache.sling.query.iterator.AdaptToIterator;
import org.apache.sling.query.resource.ResourceTreeProvider;

import aQute.bnd.annotation.ProviderType;

/**
 * SlingQuery is a Sling resource tree traversal tool inspired by the jQuery.
 */
@ProviderType
public class SlingQuery extends AbstractQuery<Resource, SlingQuery> {

	private SlingQuery(AbstractQuery<Resource, SlingQuery> original, SearchStrategy strategy) {
		super(original, strategy);
	}

	private SlingQuery(Resource[] resources, SearchStrategy strategy) {
		super(new ResourceTreeProvider(resources[0].getResourceResolver()), resources, strategy);
	}

	public static SlingQuery $(Resource... resources) {
		if (resources.length == 0) {
			throw new IllegalArgumentException("Initial collection can't be empty");
		} else {
			return new SlingQuery(resources, SearchStrategy.QUERY);
		}
	}

	public static SlingQuery $(ResourceResolver resolver) {
		return $(resolver.getResource("/"));
	}

	/**
	 * Transform the whole collection to a new {@link Iterable} object, invoking
	 * {@link Adaptable#adaptTo(Class)} method on each Resource. If some Resource can't be adapted to the
	 * class (eg. {@code adaptTo()} returns {@code null}), it will be skipped.
	 * 
	 * @param clazz Class used to adapt the Resources
	 * @return new iterable containing succesfully adapted Resources
	 */
	public <E> Iterable<E> map(final Class<? extends E> clazz) {
		return new Iterable<E>() {
			@Override
			public Iterator<E> iterator() {
				return new AdaptToIterator<Resource, E>(SlingQuery.this.iterator(), clazz);
			}
		};
	}

	@Override
	protected SlingQuery clone(AbstractQuery<Resource, SlingQuery> original, SearchStrategy strategy) {
		return new SlingQuery(original, strategy);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("$(");
		Iterator<Resource> iterator = this.iterator();
		while (iterator.hasNext()) {
			builder.append(iterator.next().getPath());
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		builder.append(")");
		return builder.toString();
	}

}