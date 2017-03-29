package org.apache.sling.samples.htlblog.models;

import java.util.Iterator;

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
	
	public Iterator<Post> getChildren() {
		Iterator<Resource> children = resource.getChildren().iterator();
		return ResourceUtil.adaptTo(children, Post.class);
	}
}