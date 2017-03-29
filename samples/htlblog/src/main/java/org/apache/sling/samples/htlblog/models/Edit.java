package org.apache.sling.samples.htlblog.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables=SlingHttpServletRequest.class)
public class Edit {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Edit.class);
	
	/** The resource resolver. */
    private ResourceResolver resourceResolver;
    
    private Post post;
    
    public Edit(SlingHttpServletRequest request) {
    	LOGGER.info("HELLO");
		resourceResolver = request.getResourceResolver();
		try {
            String path = request.getParameter("post");
            if (path != null) {
                Resource resource = resourceResolver.getResource(path);
                post = resource.adaptTo(Post.class);
            }
        } catch (Exception e) {
            LOGGER.info("Couldn't get the post to edit.", e);
        }
	}
    
    public Post getPost() {
    	return post;
    }
	
}