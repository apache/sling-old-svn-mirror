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
package org.apache.sling.servlets.get;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.servlets.get.helpers.HtmlRendererServlet;
import org.apache.sling.servlets.get.helpers.JsonRendererServlet;
import org.apache.sling.servlets.get.helpers.PlainTextRendererServlet;
import org.apache.sling.servlets.get.helpers.StreamRendererServlet;
import org.apache.sling.servlets.get.helpers.XMLRendererServlet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SlingSafeMethodsServlet that renders the current Resource as simple HTML
 * 
 * @scr.component immediate="true" label="%servlet.get.name"
 *                description="%servlet.get.description"
 * @scr.service interface="javax.servlet.Servlet"
 *
 * @scr.property name="service.description" value="Default GET Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 *
 * Use this as a default servlet for Sling
 * @scr.property name="sling.servlet.resourceTypes"
 *               value="sling/servlet/default" private="true"
 *
 * Generic handler for all get requests
 * @scr.property name="sling.servlet.methods" value="GET" private="true"
 */
public class DefaultGetServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = -5815904221043005085L;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, Servlet> rendererMap = new HashMap<String, Servlet>();

    private Servlet streamerServlet;

    /** @scr.property */
    private static final String ALIAS_PROPERTY = "aliases";

    /** @scr.property valueRef="DEFAULT_INDEX_PROPERTY" type="Boolean" */
    private static final String INDEX_PROPERTY = "index";

    private static final boolean DEFAULT_INDEX_PROPERTY = false;

    /** @scr.property valueRef="DEFAULT_INDEX_FILES_PROPERTY" */
    private static final String INDEX_FILES_PROPERTY = "index.files";

    private static final String[] DEFAULT_INDEX_FILES_PROPERTY = { "index",
        "index.html" };

    /** Default value for renderer selection (value is "true"). */
    private static final boolean DEFAULT_RENDERER_PROPERTY = true;

    /** @scr.property valueRef="DEFAULT_RENDERER_PROPERTY" type="Boolean" */
    private static final String HTML_RENDERER_PROPERTY = "enable.html";

    /** @scr.property valueRef="DEFAULT_RENDERER_PROPERTY" type="Boolean" */
    private static final String TXT_RENDERER_PROPERTY = "enable.txt";

    /** @scr.property valueRef="DEFAULT_RENDERER_PROPERTY" type="Boolean" */
    private static final String JSON_RENDERER_PROPERTY = "enable.json";

    /** @scr.property valueRef="DEFAULT_RENDERER_PROPERTY" type="Boolean" */
    private static final String XML_RENDERER_PROPERTY = "enable.xml";

    /** Additional aliases. */
    private String[] aliases;

    /** Whether to support automatic index rendering */
    private boolean index;

    /** The names of index rendering children */
    private String[] indexFiles;

    private boolean enableHtml;

    private boolean enableTxt;

    private boolean enableJson;

    private boolean enableXml;

    protected void activate(ComponentContext ctx) {
        Dictionary<?, ?> props = ctx.getProperties();
        this.aliases = OsgiUtil.toStringArray(props.get(ALIAS_PROPERTY));
        this.index = OsgiUtil.toBoolean(props.get(INDEX_PROPERTY),
            DEFAULT_INDEX_PROPERTY);
        this.indexFiles = OsgiUtil.toStringArray(
            props.get(INDEX_FILES_PROPERTY), DEFAULT_INDEX_FILES_PROPERTY);

        this.enableHtml = OsgiUtil.toBoolean(props.get(HTML_RENDERER_PROPERTY),
            DEFAULT_RENDERER_PROPERTY);
        this.enableTxt = OsgiUtil.toBoolean(props.get(TXT_RENDERER_PROPERTY),
            DEFAULT_RENDERER_PROPERTY);
        this.enableJson = OsgiUtil.toBoolean(props.get(JSON_RENDERER_PROPERTY),
            DEFAULT_RENDERER_PROPERTY);
        this.enableXml = OsgiUtil.toBoolean(props.get(XML_RENDERER_PROPERTY),
            DEFAULT_RENDERER_PROPERTY);
    }

    protected void deactivate(ComponentContext ctx) {
        this.aliases = null;
        this.index = false;
        this.indexFiles = null;
    }

    @Override
    public void init() throws ServletException {
        super.init();

        // Register renderer servlets
        setupServlet(rendererMap, StreamRendererServlet.EXT_RES,
            new StreamRendererServlet(index, indexFiles));

        if (enableHtml) {
            setupServlet(rendererMap, HtmlRendererServlet.EXT_HTML,
                new HtmlRendererServlet());
        }

        if (enableTxt) {
            setupServlet(rendererMap, PlainTextRendererServlet.EXT_TXT,
                new PlainTextRendererServlet());
        }
        
        if (enableJson) {
            setupServlet(rendererMap, JsonRendererServlet.EXT_JSON,
                new JsonRendererServlet());
        }
        
        if (enableXml) {
            setupServlet(rendererMap, XMLRendererServlet.EXT_XML,
                new XMLRendererServlet());
        }
        
        // use the servlet for rendering StreamRendererServlet.EXT_RES as the
        // streamer servlet
        streamerServlet = rendererMap.get(StreamRendererServlet.EXT_RES);

        // check additional aliases
        if (this.aliases != null) {
            for (final String m : aliases) {
                final int pos = m.indexOf(':');
                if (pos != -1) {
                    final String type = m.substring(0, pos);
                    final Servlet servlet = rendererMap.get(type);
                    if (servlet != null) {
                        final String extensions = m.substring(pos + 1);
                        final StringTokenizer st = new StringTokenizer(
                            extensions, ",");
                        while (st.hasMoreTokens()) {
                            final String ext = st.nextToken();
                            rendererMap.put(ext, servlet);
                        }
                    }
                }
            }
        }
    }

    /**
     * @throws ResourceNotFoundException if the resource of the request is a non
     *             existing resource.
     */
    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        // cannot handle the request for missing resources
        if (ResourceUtil.isNonExistingResource(request.getResource())) {
            throw new ResourceNotFoundException(
                request.getResource().getPath(), "No Resource found");
        }

        Servlet rendererServlet;
        String ext = request.getRequestPathInfo().getExtension();
        if (ext == null) {
            rendererServlet = streamerServlet;
        } else {
            rendererServlet = rendererMap.get(ext);
        }

        // fail if we should not just stream or we cannot support the ext.
        if (rendererServlet == null) {
            request.getRequestProgressTracker().log(
                "No Renderer for extension " + ext);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        request.getRequestProgressTracker().log(
            "Using " + rendererServlet.getClass().getName()
                + " to render for extension=" + ext);
        rendererServlet.service(request, response);
    }

    @Override
    public void destroy() {

        for (Servlet servlet : rendererMap.values()) {
            try {
                servlet.destroy();
            } catch (Throwable t) {
                logger.error("Error while destroying servlet " + servlet, t);
            }
        }

        streamerServlet = null;
        rendererMap.clear();

        super.destroy();
    }

    private void setupServlet(Map<String, Servlet> rendererMap, String key,
            Servlet servlet) {
        try {
            servlet.init(getServletConfig());
            rendererMap.put(key, servlet);
        } catch (Throwable t) {
            logger.error("Error while initializing servlet " + servlet, t);
        }
    }
}
