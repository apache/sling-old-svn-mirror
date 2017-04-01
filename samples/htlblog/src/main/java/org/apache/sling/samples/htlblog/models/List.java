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
package org.apache.sling.samples.htlblog.models;

import java.util.Iterator;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.iterators.ReverseListIterator;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;

@Model(adaptables=SlingHttpServletRequest.class)
public class List {
	
	private final Resource resource;
	
	public List(final SlingHttpServletRequest request) {
		ResourceResolver resourceResolver = request.getResourceResolver();
		this.resource = resourceResolver.getResource("/content/htlblog/posts");
	}
	
	@SuppressWarnings("unchecked")
	public Iterator<Post> getChildren() {
		java.util.List<Resource> childrenList = IteratorUtils.toList(this.resource.getChildren().iterator());
        Iterator<Resource> reverseChildren = new ReverseListIterator(childrenList);
		return ResourceUtil.adaptTo(reverseChildren, Post.class);
	}
}