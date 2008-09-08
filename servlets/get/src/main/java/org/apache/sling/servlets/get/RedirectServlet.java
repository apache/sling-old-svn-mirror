/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.servlets.get;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.get.helpers.JsonRendererServlet;

/**
 * The <code>RedirectServlet</code> implements support for GET requests to
 * resources of type <code>sling:redirect</code>. This servlet accesses the
 * resource <code>sling:target</code> below the requested resource and tries
 * to redirect to the target resource:
 * <ul>
 * <li>If the <code>sling:target</code> resource is based on a JCR property
 * of type <em>REFERENCE</em> the path of the target node is used as the
 * target for the redirection.
 * <li>Otherwise the <code>sling:target</code> resource is adapted to a
 * String which is taken as the path to the target for the redirection if not
 * <code>null</code>.
 * </ul>
 * <p>
 * If there is no <code>sling:target</code> child resource or the resource
 * does not adapt to a JCR Node or a (path) String a 404 (NOT FOUND) status is
 * sent by this servlet. Otherwise a 302 (FOUND, temporary redirect) status is
 * sent where the target is the relative URL from the current resource to the
 * target resource. Selectors, extension, suffix and query string are also
 * appended to the redirect URL.
 *
 * @scr.component immediate="true" metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 *
 * @scr.property name="service.description" value="Request Redirect Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 *
 * @scr.property name="sling.servlet.resourceTypes" value="sling:redirect"
 * @scr.property name="sling.servlet.methods" value="GET"
 */
public class RedirectServlet extends SlingSafeMethodsServlet {

    /** The name of the target property */
    public static final String TARGET_PROP = "sling:target";

    private Servlet jsonRendererServlet;

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        // handle json export of the redirect node
        if (JsonRendererServlet.EXT_JSON.equals(request.getRequestPathInfo().getExtension())) {
            getJsonRendererServlet().service(request, response);
            return;
        }

        String targetPath = null;

        // convert resource to a value map
        final Resource rsrc = request.getResource();
        final ValueMap valueMap = rsrc.adaptTo(ValueMap.class);
        if ( valueMap != null ) {
            targetPath = valueMap.get(TARGET_PROP, String.class);
        }
        if ( targetPath == null ) {
            // old behaviour
            final Resource targetResource = request.getResourceResolver().getResource(
                    rsrc, TARGET_PROP);
            if (targetResource == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Missing target for redirection");
                return;
            }


            // if the target resource is a reference, we can adapt to node
            Node targetNode = targetResource.adaptTo(Node.class);
            if (targetNode != null) {

                // get the node path (aka resource path)
                try {
                    targetPath = targetNode.getPath();
                } catch (RepositoryException re) {
                    throw new ServletException(
                        "Failed to access repository for redirection", re);

                }

            } else {

                // if the target resource is a path (string), redirect there
                targetPath = targetResource.adaptTo(String.class);

            }
        }

        // if we got a target path, make it external and redirect to it
        if (targetPath != null) {
            // make path relative and append selectors, extension etc.
            targetPath = toRedirectPath(targetPath, request);

            // and redirect there ...
            response.sendRedirect(targetPath);

            return;
        }

        // no way of finding the target, just fail
        response.sendError(HttpServletResponse.SC_NOT_FOUND,
            "Cannot redirect to target resource " + targetPath);
    }

    /**
     * Create a relative redirect URL for the targetPath relative to the given
     * request. The URL is relative to the request's resource and will include
     * the selectors, extension, suffix and query string of the request.
     */
    protected static String toRedirectPath(String targetPath,
            SlingHttpServletRequest request) {

        String postFix;
        RequestPathInfo rpi = request.getRequestPathInfo();
        if (rpi.getExtension() != null) {
            StringBuffer postfixBuf = new StringBuffer();
            if (rpi.getSelectorString() != null) {
                postfixBuf.append('.').append(rpi.getSelectorString());
            }
            postfixBuf.append('.').append(rpi.getExtension());
            if (rpi.getSuffix() != null) {
                postfixBuf.append(rpi.getSuffix());
            }
            postFix = postfixBuf.toString();
        } else {
            postFix = null;
        }

        String basePath = request.getResource().getPath();

        // make sure the target path is absolute
        if (!targetPath.startsWith("/")) {
            if (!basePath.endsWith("/")) {
                targetPath = "/".concat(targetPath);
            }
            targetPath = basePath.concat(targetPath);
        }

        // append optional selectors etc.to the base path
        if (postFix != null) {
            basePath = basePath.concat(postFix);
        }

        StringBuffer pathBuf = new StringBuffer();

        makeRelative(pathBuf, basePath, targetPath);

        if (postFix != null) {
            pathBuf.append(postFix);
        }

        if (request.getQueryString() != null) {
            pathBuf.append('?').append(request.getQueryString());
        }

        return pathBuf.toString();
    }

    /**
     * Converts the absolute path target into a path relative to base and stores
     * this relative path into pathBuffer.
     */
    private static void makeRelative(StringBuffer pathBuffer, String base,
            String target) {

        String[] bParts = base.substring(1).split("/");
        String[] tParts = target.substring(1).split("/");

        // find first non-matching part
        int off;
        for (off = 0; off < (bParts.length - 1) && off < tParts.length
            && bParts[off].equals(tParts[off]); off++);

        for (int i = bParts.length - off; i > 1; i--) {
            pathBuffer.append("../");
        }

        for (int i = off; i < tParts.length; i++) {
            if (i > off) {
                pathBuffer.append('/');
            }
            pathBuffer.append(tParts[i]);
        }
    }

    private Servlet getJsonRendererServlet() {
        if (jsonRendererServlet == null) {
            Servlet jrs = new JsonRendererServlet();
            try {
                jrs.init(getServletConfig());
            } catch (Exception e) {
                // don't care too much here
            }
            jsonRendererServlet = jrs;
        }
        return jsonRendererServlet;
    }
}
