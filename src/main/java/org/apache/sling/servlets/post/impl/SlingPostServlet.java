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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.servlets.post.impl.helper.DateParser;
import org.apache.sling.servlets.post.impl.helper.NodeNameGenerator;
import org.apache.sling.servlets.post.impl.operations.CopyOperation;
import org.apache.sling.servlets.post.impl.operations.DeleteOperation;
import org.apache.sling.servlets.post.impl.operations.ModifyOperation;
import org.apache.sling.servlets.post.impl.operations.MoveOperation;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POST servlet that implements the sling client library "protocol"
 *
 * @scr.component immediate="true" label="%servlet.post.name"
 *                description="%servlet.post.description"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value="Sling Post Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 *
 * Use this as the default servlet for POST requests for Sling
 * @scr.property name="sling.servlet.resourceTypes"
 *               value="sling/servlet/default" private="true"
 * @scr.property name="sling.servlet.methods" value="POST" private="true"
 * @scr.reference name="postProcessors" interface="org.apache.sling.servlets.post.SlingPostProcessor" cardinality="0..n" policy="dynamic"
 */
public class SlingPostServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1837674988291697074L;

    /**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * @scr.property values.0="EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
     *               values.1="yyyy-MM-dd'T'HH:mm:ss.SSSZ"
     *               values.2="yyyy-MM-dd'T'HH:mm:ss" values.3="yyyy-MM-dd"
     *               values.4="dd.MM.yyyy HH:mm:ss" values.5="dd.MM.yyyy"
     */
    private static final String PROP_DATE_FORMAT = "servlet.post.dateFormats";

    /**
     * @scr.property values.0="title" values.1="jcr:title" values.2="name"
     *               values.3="description" values.4="jcr:description"
     *               values.5="abstract"
     */
    private static final String PROP_NODE_NAME_HINT_PROPERTIES = "servlet.post.nodeNameHints";

    /**
     * @scr.property value="20" type="Integer"
     */
    private static final String PROP_NODE_NAME_MAX_LENGTH = "servlet.post.nodeNameMaxLength";

    /**
     * utility class for generating node names
     */
    private NodeNameGenerator nodeNameGenerator;

    /**
     * utility class for parsing date strings
     */
    private DateParser dateParser;

    private SlingPostOperation modifyOperation;

    private final Map<String, SlingPostOperation> postOperations = new HashMap<String, SlingPostOperation>();

    private final List<ServiceReference> delayedPostProcessors = new ArrayList<ServiceReference>();

    private final List<ServiceReference> postProcessors = new ArrayList<ServiceReference>();

    private SlingPostProcessor[] cachedPostProcessors = new SlingPostProcessor[0];

    private ComponentContext componentContext;

    @Override
    public void init() {
        // default operation: create/modify
        modifyOperation = new ModifyOperation(nodeNameGenerator, dateParser,
            getServletContext());

        // other predefined operations
        postOperations.put(SlingPostConstants.OPERATION_COPY,
            new CopyOperation());
        postOperations.put(SlingPostConstants.OPERATION_MOVE,
            new MoveOperation());
        postOperations.put(SlingPostConstants.OPERATION_DELETE,
            new DeleteOperation());
    }

    @Override
    public void destroy() {
        modifyOperation = null;
        postOperations.clear();
    }

    @Override
    protected void doPost(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        // prepare the response
        HtmlResponse htmlResponse = new HtmlResponse();
        htmlResponse.setReferer(request.getHeader("referer"));

        SlingPostOperation operation = getSlingPostOperation(request);
        if (operation == null) {

            htmlResponse.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Invalid operation specified for POST request");

        } else {

            final SlingPostProcessor[] processors;
            synchronized ( this.delayedPostProcessors ) {
                processors = this.cachedPostProcessors;
            }
            try {
                operation.run(request, htmlResponse, processors);
            } catch (ResourceNotFoundException rnfe) {
                htmlResponse.setStatus(HttpServletResponse.SC_NOT_FOUND,
                    rnfe.getMessage());
            } catch (Throwable throwable) {
                htmlResponse.setError(throwable);
            }

        }

        // check for redirect URL if processing succeeded
        if (htmlResponse.isSuccessful()) {
            String redirect = getRedirectUrl(request, htmlResponse);
            if (redirect != null) {
                response.sendRedirect(redirect);
                return;
            }
        }

        // create a html response and send if unsuccessful or no redirect
        htmlResponse.send(response, isSetStatus(request));
    }

    private SlingPostOperation getSlingPostOperation(
            SlingHttpServletRequest request) {
        String operation = request.getParameter(SlingPostConstants.RP_OPERATION);
        if (operation == null || operation.length() == 0) {
            // standard create/modify operation;
            return modifyOperation;
        }

        // named operation, retrieve from map
        return postOperations.get(operation);
    }

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

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        synchronized ( this.delayedPostProcessors ) {
            this.componentContext = context;
            for(final ServiceReference ref : this.delayedPostProcessors) {
                this.registerPostProcessor(ref);
            }
            this.delayedPostProcessors.clear();
        }
        Dictionary<?, ?> props = context.getProperties();

        String[] nameHints = OsgiUtil.toStringArray(props.get(PROP_NODE_NAME_HINT_PROPERTIES));
        int nameMax = (int) OsgiUtil.toLong(
            props.get(PROP_NODE_NAME_MAX_LENGTH), -1);
        nodeNameGenerator = new NodeNameGenerator(nameHints, nameMax);

        dateParser = new DateParser();
        String[] dateFormats = OsgiUtil.toStringArray(props.get(PROP_DATE_FORMAT));
        for (String dateFormat : dateFormats) {
            dateParser.register(dateFormat);
        }
    }

    protected void deactivate(ComponentContext context) {
        nodeNameGenerator = null;
        dateParser = null;
        this.componentContext = null;
    }

    protected void bindPostProcessors(ServiceReference ref) {
        synchronized ( this.delayedPostProcessors ) {
            if ( this.componentContext == null ) {
                this.delayedPostProcessors.add(ref);
            } else {
                this.registerPostProcessor(ref);
            }
        }
    }

    protected void unbindPostProcessors(ServiceReference ref) {
        synchronized ( this.delayedPostProcessors ) {
            this.delayedPostProcessors.remove(ref);
            this.postProcessors.remove(ref);
        }
    }

    protected void registerPostProcessor(ServiceReference ref) {
        final int ranking = OsgiUtil.toInteger(ref.getProperty(Constants.SERVICE_RANKING), 0);
        int index = 0;
        while ( index < this.postProcessors.size() &&
                ranking < OsgiUtil.toInteger(this.postProcessors.get(index).getProperty(Constants.SERVICE_RANKING), 0)) {
            index++;
        }
        if ( index == this.postProcessors.size() ) {
            this.postProcessors.add(ref);
        } else {
            this.postProcessors.add(index, ref);
        }
        this.cachedPostProcessors = new SlingPostProcessor[this.postProcessors.size()];
        index = 0;
        for(final ServiceReference current : this.postProcessors) {
            final SlingPostProcessor processor = (SlingPostProcessor) this.componentContext.locateService("postProcessor", current);
            this.cachedPostProcessors[index] = processor;
            index++;
        }
    }
}
