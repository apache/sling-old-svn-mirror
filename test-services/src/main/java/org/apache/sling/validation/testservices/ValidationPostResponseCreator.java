package org.apache.sling.validation.testservices;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.PostResponseCreator;
import org.apache.sling.servlets.post.SlingPostConstants;

@Component()
@Service(PostResponseCreator.class)
public class ValidationPostResponseCreator implements PostResponseCreator {

    @Override
    public PostResponse createPostResponse(SlingHttpServletRequest request) {
        String operation = request.getParameter(SlingPostConstants.RP_OPERATION);
        if (operation != null && "validation".equals(operation)) {
            return new ValidationPostResponse();
        }
        return null;
    }
}
