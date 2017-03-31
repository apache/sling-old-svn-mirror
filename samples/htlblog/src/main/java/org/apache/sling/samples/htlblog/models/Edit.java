package org.apache.sling.samples.htlblog.models;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
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
    	this.resourceResolver = request.getResourceResolver();
		try {
            String path = request.getParameter("post");
            if (path != null) {
                Resource resource = this.resourceResolver.getResource(path);
                this.post = resource.adaptTo(Post.class);
            }
        } catch (Exception e) {
            LOGGER.info("Couldn't get the post to edit.", e);
        }
	}
    
    public Post getPost() {
    	return post;
    }
    
    public boolean canEdit() {
    	boolean canEdit = false;
    	JackrabbitSession jackrabbitSession = ((JackrabbitSession) resourceResolver.adaptTo(Session.class));
    	try {
            User user = (User) jackrabbitSession.getUserManager().getAuthorizable(jackrabbitSession.getUserID());
            if(user.isAdmin()) {
            	canEdit = true;
            }     
        } catch (RepositoryException e) {
            LOGGER.error("Could not get user.", e);
        }
    	return canEdit;
    }
	
}