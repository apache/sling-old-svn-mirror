package org.apache.sling.samples.htlblog.models;

import java.util.Iterator;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;

@Model(adaptables=Resource.class)
public class Post {
	
	private final Resource resource;
	
	@Inject
	private String title;
	
	@Inject
	private String body;
	
	public Post(final Resource resource) {
		this.resource = resource;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getBody() {
		return body;
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
}