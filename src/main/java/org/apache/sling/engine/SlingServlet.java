package org.apache.sling.engine;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * Interface to the main Sling Servlet, that allows for running requests outside
 * of the servlet container's HTTP request/response cycle.
 */
public interface SlingServlet {
    /**
     * Process an HTTP request
     * 
     * @param request
     *            Usually a "synthetic" request, i.e. not supplied by servlet
     *            container
     * @param resource
     *            Usually a "synthetic" response, i.e. not supplied by servlet
     *            container
     * @param resourceResolver
     *            A "fresh" ResourceResolver is needed to process a request
     *            outside of the HTTP request/response cycle. It is usually
     *            created based on the AuthenticationInfo from the original
     *            request.
     */
    void processRequest(HttpServletRequest request,
            HttpServletResponse resource, ResourceResolver resourceResolver)
            throws IOException;
}