package org.apache.sling.samples.htlblog.models;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

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
}