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
package org.apache.sling.servlets.post.impl;

import java.io.IOException;

import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POST servlet that implements the sling client library "protocol"
 *
 * @scr.service
 *  interface="javax.servlet.Servlet"
 *
 * @scr.component
 *  immediate="true"
 *  metatype="false"
 *
 * @scr.property
 *  name="service.description"
 *  value="Sling Post Servlet"
 *
 * @scr.property
 *  name="service.vendor"
 *  value="The Apache Software Foundation"
 *
 * Use this as the default servlet for POST requests for Sling
 * @scr.property
 *  name="sling.servlet.resourceTypes"
 *  value="sling/servlet/default"
 * @scr.property
 *  name="sling.servlet.methods"
 *  value="POST"
 */
public class SlingPostServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1837674988291697074L;

    /**
     * default log
     */
    private static final Logger log = LoggerFactory.getLogger(SlingPostServlet.class);

    /**
     * Prefix for parameter names which control this POST
     * (RP_ stands for "request param")
     */
    public static final String RP_PREFIX = ":";

    /**
     * suffix that indicates node creation
     */
    public static final String DEFAULT_CREATE_SUFFIX = "/*";

    /**
     * Optional request parameter: delete the specified content paths
     */
    public static final String RP_DELETE_PATH = RP_PREFIX + "delete";

    /**
     * Optional request parameter: move the specified content paths
     */
    public static final String RP_MOVE_SRC = RP_PREFIX + "moveSrc";

    /**
     * Optional request parameter: move the specified content paths to this
     * destination
     */
    public static final String RP_MOVE_DEST = RP_PREFIX + "moveDest";

    /**
     * Optional request parameter: move flags
     */
    public static final String RP_MOVE_FLAGS = RP_PREFIX + "moveFlags";

    /**
     * Optional request parameter: copy the specified content paths
     */
    public static final String RP_COPY_SRC = RP_PREFIX + "copySrc";

    /**
     * Optional request parameter: copy the specified content paths to this
     * destination
     */
    public static final String RP_COPY_DEST = RP_PREFIX + "copyDest";

    /**
     * Optional request parameter: copy flags
     */
    public static final String RP_COPY_FLAGS = RP_PREFIX + "copyFlags";

    /**
     * name of the 'replace' move/copy flag
     */
    public static final String FLAG_REPLACE = "replace";

    /**
     * Optional request paramter specifying a node name for a newly created node.
     */
    public static final String RP_NODE_NAME = RP_PREFIX + "name";

    /**
     * Optional request paramter specifying a node hint for a newly created node.
     */
    public static final String RP_NODE_NAME_HINT = RP_PREFIX + "nameHint";

    /**
     * Optional request parameter: only request parameters starting with this prefix are
     * saved as Properties when creating a Node. Active only if at least one parameter
     * starts with this prefix, and defaults to {@link #DEFAULT_SAVE_PARAM_PREFIX}.
     */
    public static final String RP_SAVE_PARAM_PREFIX = RP_PREFIX + "saveParamPrefix";

    /**
     * Default value for {@link #RP_SAVE_PARAM_PREFIX}
     */
    public static final String DEFAULT_SAVE_PARAM_PREFIX = "./";

    /**
     * Optional request parameter: if value is 0, created node is ordered so as
     * to be the first child of its parent.
     */
    public static final String RP_ORDER = RP_PREFIX + "order";

    /**
     * Optional request parameter: redirect to the specified URL after POST
     */
    public static final String RP_REDIRECT_TO =  RP_PREFIX + "redirect";

    /**
     * Optional request parameter: if provided, added at the end of the computed
     * (or supplied) redirect URL
     */
    public static final String RP_DISPLAY_EXTENSION = RP_PREFIX + "displayExtension";

    /**
     * SLING-130, suffix that maps form field names to different JCR property names
     */
    public static final String VALUE_FROM_SUFFIX = "@ValueFrom";

    /**
     * suffix that indicates a type hint parameter
     */
    public static final String TYPE_HINT_SUFFIX = "@TypeHint";

    /**
     * suffix that indicates a default value parameter
     */
    public static final String DEFAULT_VALUE_SUFFIX = "@DefaultValue";

    /**
     * utility class for generating node names
     */
    private final NodeNameGenerator nodeNameGenerator = new NodeNameGenerator();

    /**
     * utility class for parsing date strings
     */
    private final DateParser dateParser = new DateParser(); {
        // TODO: maybe put initialization to OSGI activation ?
        dateParser.register("EEE MMM dd yyyy HH:mm:ss 'GMT'Z");
        dateParser.register("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateParser.register("yyyy-MM-dd'T'HH:mm:ss");
        dateParser.register("yyyy-MM-dd");
        dateParser.register("dd.MM.yyyy HH:mm:ss");
        dateParser.register("dd.MM.yyyy");
    }


    @Override
    protected void doPost(SlingHttpServletRequest request,
                          SlingHttpServletResponse response)
            throws ServletException, IOException {

        // create a post processor and process changes
        SlingPostProcessor p = createPostProcessor(request);
        p.run();
        HtmlResponse resp = p.getHtmlResponse();

        // check for redirect url
        String redirect = getRedirectUrl(request, resp);
        if (redirect != null && resp.getError() == null) {
            response.sendRedirect(redirect);
        } else {
            // create a html response and send
            resp.send(response, false);
        }
    }

    /**
     * Creats a post processor for the given request.
     * @param request the request for the processor
     * @return a post context
     * @throws ServletException if no session can be aquired or if there is a
     *         repository error.
     */
    private SlingPostProcessor createPostProcessor(SlingHttpServletRequest request)
            throws ServletException {
        Session s = request.getResourceResolver().adaptTo(Session.class);
        if (s == null) {
            throw new ServletException("No JCR Session available");
        }

        return new SlingPostProcessor(request, s, nodeNameGenerator, dateParser, this.getServletContext());
    }

    /**
     * compute redirect URL (SLING-126)
     * @param ctx the post processor
     * @return the redirect location or <code>null</code>
     */
    protected String getRedirectUrl(HttpServletRequest request, HtmlResponse ctx) {
        // redirect param has priority (but see below, magic star)
        String result = request.getParameter(RP_REDIRECT_TO);
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
            }

            if (log.isDebugEnabled()) {
                log.debug("Will redirect to " + result);
            }
        }
        return result;
    }

}

