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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

@Model(adaptables=Resource.class)
public class Post {
	
	private final Resource resource;
	
	@Inject
	private String title;
	
	@Inject
	private String body;
	
	@Inject @Optional
    private Calendar created;
	
	public Post(final Resource resource) {
		this.resource = resource;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getBody() {
		return body;
	}
	
	public String getCreated() {
		SimpleDateFormat formatter = new SimpleDateFormat("MMMM, d yyyy");
		return formatter.format(created.getTime());
	}
	
	public String getPath() {
    	return resource.getPath();
    }
	
	public String getUrl() {
		return getPath() + ".html";
	}
	
	public Iterator<Post> getChildren() {
		Iterator<Resource> children = resource.getChildren().iterator();
		return ResourceUtil.adaptTo(children, Post.class);
	}
	
	public String getFeaturedImagePath() {
		Iterator<Resource> featuredChildren = resource.getChild("featuredImage").listChildren();
		return featuredChildren.next().getPath();
	}
}