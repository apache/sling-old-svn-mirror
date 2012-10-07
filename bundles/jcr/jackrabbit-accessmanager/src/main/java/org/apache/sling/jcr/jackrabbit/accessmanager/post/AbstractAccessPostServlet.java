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
package org.apache.sling.jcr.jackrabbit.accessmanager.post;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.impl.helper.JSONResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all the POST servlets for the AccessManager operations
 */
public abstract class AbstractAccessPostServlet extends SlingAllMethodsServlet {
	private static final long serialVersionUID = -5918670409789895333L;

	/**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

	/* (non-Javadoc)
	 * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
	 */
	@Override
	protected void doPost(SlingHttpServletRequest request,
			SlingHttpServletResponse httpResponse) throws ServletException,
			IOException {
        // prepare the response
        HtmlResponse htmlResponse = createHtmlResponse(request);
        htmlResponse.setReferer(request.getHeader("referer"));

        // calculate the paths
        String path = getItemPath(request);
        htmlResponse.setPath(path);

        // location
        htmlResponse.setLocation(externalizePath(request, path));

        // parent location
        path = ResourceUtil.getParent(path);
        if (path != null) {
        	htmlResponse.setParentLocation(externalizePath(request, path));
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);

        final List<Modification> changes = new ArrayList<Modification>();

        try {
            handleOperation(request, htmlResponse, changes);

            //TODO: maybe handle SlingAuthorizablePostProcessor handlers here

            // set changes on html response
            for(Modification change : changes) {
                switch ( change.getType() ) {
                    case MODIFY : htmlResponse.onModified(change.getSource()); break;
                    case DELETE : htmlResponse.onDeleted(change.getSource()); break;
                    case MOVE :   htmlResponse.onMoved(change.getSource(), change.getDestination()); break;
                    case COPY :   htmlResponse.onCopied(change.getSource(), change.getDestination()); break;
                    case CREATE : htmlResponse.onCreated(change.getSource()); break;
                    case ORDER : htmlResponse.onChange("ordered", change.getSource(), change.getDestination()); break;
				default:
					break;
                }
            }

            if (session.hasPendingChanges()) {
                session.save();
            }
        } catch (ResourceNotFoundException rnfe) {
            htmlResponse.setStatus(HttpServletResponse.SC_NOT_FOUND,
                rnfe.getMessage());
        } catch (Throwable throwable) {
            log.debug("Exception while handling POST "
                + request.getResource().getPath() + " with "
                + getClass().getName(), throwable);
            htmlResponse.setError(throwable);
        } finally {
            try {
                if (session.hasPendingChanges()) {
                    session.refresh(false);
                }
            } catch (RepositoryException e) {
                log.warn("RepositoryException in finally block: {}",
                    e.getMessage(), e);
            }
        }

        // check for redirect URL if processing succeeded
        if (htmlResponse.isSuccessful()) {
            String redirect = getRedirectUrl(request, htmlResponse);
            if (redirect != null) {
                httpResponse.sendRedirect(redirect);
                return;
            }
        }

        // create a html response and send if unsuccessful or no redirect
        htmlResponse.send(httpResponse, isSetStatus(request));
	}

    /**
     * Creates an instance of a HtmlResponse.
     * @param req The request being serviced
     * @return a {@link org.apache.sling.servlets.post.impl.helper.JSONResponse} if any of these conditions are true:
     * <ul>
     *   <li>the response content type is application/json
     * </ul>
     * or a {@link org.apache.sling.api.servlets.HtmlResponse} otherwise
     */
    protected HtmlResponse createHtmlResponse(SlingHttpServletRequest req) {
    	if (JSONResponse.RESPONSE_CONTENT_TYPE.equals(req.getResponseContentType())) {
    		return new JSONResponse();
    	} else {
            return new HtmlResponse();
    	}
    }
	
	/**
	 * Extending Servlet should implement this operation to do the work
	 *
	 * @param request the sling http request to process
	 * @param htmlResponse the response
	 * @param changes
	 */
	abstract protected void handleOperation(SlingHttpServletRequest request,
			HtmlResponse htmlResponse, List<Modification> changes) throws RepositoryException;


    /**
     * compute redirect URL (SLING-126)
     *
     * @param ctx the post processor
     * @return the redirect location or <code>null</code>
     */
    protected String getRedirectUrl(HttpServletRequest request, HtmlResponse ctx) {
        // redirect param has priority (but see below, magic star)
        String result = request.getParameter(SlingPostConstants.RP_REDIRECT_TO);
        if (result != null && ctx.getPath() != null) {

            // redirect to created/modified Resource
            int star = result.indexOf('*');
            if (star >= 0) {
                StringBuffer buf = new StringBuffer();

                // anything before the star
                if (star > 0) {
                    buf.append(result.substring(0, star));
                }

                // append the name of the manipulated node
                buf.append(ResourceUtil.getName(ctx.getPath()));

                // anything after the star
                if (star < result.length() - 1) {
                    buf.append(result.substring(star + 1));
                }

                // use the created path as the redirect result
                result = buf.toString();

            } else if (result.endsWith(SlingPostConstants.DEFAULT_CREATE_SUFFIX)) {
                // if the redirect has a trailing slash, append modified node
                // name
                result = result.concat(ResourceUtil.getName(ctx.getPath()));
            }

            if (log.isDebugEnabled()) {
                log.debug("Will redirect to " + result);
            }
        }
        return result;
    }

    protected boolean isSetStatus(SlingHttpServletRequest request) {
        String statusParam = request.getParameter(SlingPostConstants.RP_STATUS);
        if (statusParam == null) {
            log.debug(
                "getStatusMode: Parameter {} not set, assuming standard status code",
                SlingPostConstants.RP_STATUS);
            return true;
        }

        if (SlingPostConstants.STATUS_VALUE_BROWSER.equals(statusParam)) {
            log.debug(
                "getStatusMode: Parameter {} asks for user-friendly status code",
                SlingPostConstants.RP_STATUS);
            return false;
        }

        if (SlingPostConstants.STATUS_VALUE_STANDARD.equals(statusParam)) {
            log.debug(
                "getStatusMode: Parameter {} asks for standard status code",
                SlingPostConstants.RP_STATUS);
            return true;
        }

        log.debug(
            "getStatusMode: Parameter {} set to unknown value {}, assuming standard status code",
            SlingPostConstants.RP_STATUS);
        return true;
    }

	// ------ These methods were copied from AbstractSlingPostOperation ------

    /**
     * Returns the path of the resource of the request as the item path.
     * <p>
     * This method may be overwritten by extension if the operation has
     * different requirements on path processing.
     */
    protected String getItemPath(SlingHttpServletRequest request) {
        return request.getResource().getPath();
    }

    /**
     * Returns an external form of the given path prepending the context path
     * and appending a display extension.
     *
     * @param path the path to externalize
     * @return the url
     */
    protected final String externalizePath(SlingHttpServletRequest request,
            String path) {
        StringBuffer ret = new StringBuffer();
        ret.append(SlingRequestPaths.getContextPath(request));
        ret.append(request.getResourceResolver().map(path));

        // append optional extension
        String ext = request.getParameter(SlingPostConstants.RP_DISPLAY_EXTENSION);
        if (ext != null && ext.length() > 0) {
            if (ext.charAt(0) != '.') {
                ret.append('.');
            }
            ret.append(ext);
        }

        return ret.toString();
    }

    /**
     * Returns an <code>AccessControlList</code> to edit for the node at the
     * given <code>resourcePath</code>.
     *
     * @param accessControlManager The manager providing access control lists
     * @param resourcePath The node path for which to return an access control
     *            list
     * @param mayCreate <code>true</code> if an access control list should be
     *            created if the node does not have one yet.
     * @return The <code>AccessControlList</code> to modify to control access to
     *         the node.
     * @throws RepositoryException If the access control manager does not
     *             provide a <code>AccessControlPolicy</code> which is an
     *             <code>AccessControlList</code>.
     */
    protected AccessControlList getAccessControlList(
            final AccessControlManager accessControlManager,
            final String resourcePath, final boolean mayCreate)
            throws RepositoryException {

        // check for an existing access control list to edit
        AccessControlPolicy[] policies = accessControlManager.getPolicies(resourcePath);
        for (AccessControlPolicy policy : policies) {
            if (policy instanceof AccessControlList) {
                return (AccessControlList) policy;
            }
        }

        // no existing access control list, try to create if allowed
        if (mayCreate) {
            AccessControlPolicyIterator applicablePolicies = accessControlManager.getApplicablePolicies(resourcePath);
            while (applicablePolicies.hasNext()) {
                AccessControlPolicy policy = applicablePolicies.nextAccessControlPolicy();
                if (policy instanceof AccessControlList) {
                    return (AccessControlList) policy;
                }
            }
        }

        // neither an existing nor a create AccessControlList is available, fail
        throw new RepositoryException(
            "Unable to find or create an access control policy to update for "
                + resourcePath);

    }
}
