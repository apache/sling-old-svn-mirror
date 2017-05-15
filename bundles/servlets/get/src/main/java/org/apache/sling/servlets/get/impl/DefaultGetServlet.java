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
package org.apache.sling.servlets.get.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.get.impl.helpers.HeadServletResponse;
import org.apache.sling.servlets.get.impl.helpers.HtmlRendererServlet;
import org.apache.sling.servlets.get.impl.helpers.JsonRendererServlet;
import org.apache.sling.servlets.get.impl.helpers.PlainTextRendererServlet;
import org.apache.sling.servlets.get.impl.helpers.StreamRendererServlet;
import org.apache.sling.servlets.get.impl.helpers.XMLRendererServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SlingSafeMethodsServlet that renders the current Resource as simple HTML
 */
@Component(service = Servlet.class,
    name="org.apache.sling.servlets.get.DefaultGetServlet",
    property = {
            "service.description=Default GET Servlet",
            "service.vendor=The Apache Software Foundation",

            // Use this as a default servlet for Sling
            "sling.servlet.resourceTypes=sling/servlet/default",
            "sling.servlet.prefix:Integer=-1",

            // Generic handler for all get requests
            "sling.servlet.methods=GET",
            "sling.servlet.methods=HEAD"
    })
@Designate(ocd=DefaultGetServlet.Config.class)
public class DefaultGetServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = -2714152339750885354L;

    @ObjectClassDefinition(name="Apache Sling GET Servlet",
            description="The Sling GET servlet is registered as the default servlet to handle GET requests.")
    public @interface Config {

        @AttributeDefinition(name = "Extension Aliases",
                description="The aliases can be used to map several extensions to a " +
                            "single servlet. For instance \"xml:pdf,rtf\" maps the extensions \".pdf\" and " +
                            "\".rtf\" to the servlet helper handling the \".xml\" extension.")
        String[] aliases();

        @AttributeDefinition(name = "Auto Index",
                description="Controls whether a simple directory index is rendered for " +
                             "a directory request. A directory request is a request to a resource with a " +
                             "trailing slash (/) character, for example http://host/apps/. If none of the " +
                             "index resources exists, the default GET servlet may automatically render an " +
                             "index listing of the child resources if this option is checked, which is the " +
                             "default. If this option is not checked, the request to the resource is " +
                             "forbidden and results in a status 403/FORBIDDEN. This configuration " +
                             "corresponds to the \"Index\" option of the Options directive of Apache HTTP " +
                             "Server (httpd).")
        boolean index() default false;

        @AttributeDefinition(name = "Index Resources",
                description = "List of child resources to be considered for rendering  " +
                             "the index of a \"directory\". The default value is [ \"index\", \"index.html\" ].  " +
                             "Each entry in the list is checked and the first entry found is included to  " +
                             "render the index. If an entry is selected, which has not extension (for  " +
                             "example the \"index\" resource), the extension \".html\" is appended for the  " +
                             "inclusion to indicate the desired text/html rendering. If the resource name  " +
                             "has an extension (as in \"index.html\"), no additional extension is appended  " +
                             "for the inclusion. This configuration corresponds to the <DirectoryIndex>  " +
                             "directive of Apache HTTP Server (httpd).")
        String[] index_files() default { "index","index.html" };

        @AttributeDefinition(name = "Enable HTML",
                description = "Whether the renderer for HTML of the default GET servlet is enabled or not. By default the HTML renderer is enabled.")
        boolean enable_html() default true;

        @AttributeDefinition(name = "Enable JSON",
                description = "Whether the renderer for JSON of the default GET servlet is enabled or not. By default the JSON renderer is enabled.")
        boolean enable_json() default true;

        @AttributeDefinition(name = "Enable Plain Text",
                description = "Whether the renderer for plain text of the default GET servlet is enabled or not. By default the plain text renderer is enabled.")
        boolean enable_txt() default true;

        @AttributeDefinition(name = "Enable XML",
                description = "Whether the renderer for XML of the default GET servlet is enabled or not. By default the XML renderer is enabled.")
        boolean enable_xml() default true;

        @AttributeDefinition(name = "JSON Max results",
                description = "The maximum number of resources that should " +
                  "be returned when doing a node.5.json or node.infinity.json. In JSON terms " +
                  "this basically means the number of Objects to return. Default value is " +
                  "200.")
        int json_maximumresults() default 200;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, Servlet> rendererMap = new HashMap<>();

    private Servlet streamerServlet;

    private int jsonMaximumResults;

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

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private XSSAPI xssApi;

    @Activate
    protected void activate(Config cfg) {
        this.aliases = cfg.aliases();
        this.index = cfg.index();
        this.indexFiles = cfg.index_files();
        if ( this.indexFiles == null ) {
            this.indexFiles = new String[0];
        }

        this.enableHtml = cfg.enable_html();
        this.enableTxt = cfg.enable_txt();
        this.enableJson = cfg.enable_json();
        this.enableXml = cfg.enable_xml();
        this.jsonMaximumResults = cfg.json_maximumresults();
    }

    @Deactivate
    protected void deactivate() {
        this.aliases = null;
        this.index = false;
        this.indexFiles = null;
    }

    private Servlet getDefaultRendererServlet(final String type) {
        Servlet servlet = null;
        if ( StreamRendererServlet.EXT_RES.equals(type) ) {
            servlet = new StreamRendererServlet(index, indexFiles);
        } else if ( HtmlRendererServlet.EXT_HTML.equals(type) ) {
            servlet = new HtmlRendererServlet(xssApi);
        } else if ( PlainTextRendererServlet.EXT_TXT.equals(type) ) {
            servlet = new PlainTextRendererServlet();
        } else if (JsonRendererServlet.EXT_JSON.equals(type) ) {
            servlet = new JsonRendererServlet(jsonMaximumResults);
        } else if ( XMLRendererServlet.EXT_XML.equals(type) ) {
            try {
                servlet = new XMLRendererServlet();
            } catch (Throwable t) {
                logger.warn("Support for getting XML is currently disabled " +
                        "in the servlets get module. Check whether the JCR API is available.");
            }
        }
        if ( servlet != null ) {
            try {
                servlet.init(getServletConfig());
            } catch (Throwable t) {
                logger.error("Error while initializing servlet " + servlet, t);
                servlet = null;
            }
        }
        return servlet;
    }

    @Override
    public void init() throws ServletException {
        super.init();

        // use the servlet for rendering StreamRendererServlet.EXT_RES as the
        // streamer servlet
        streamerServlet = getDefaultRendererServlet(StreamRendererServlet.EXT_RES);

        // Register renderer servlets
        rendererMap.put(StreamRendererServlet.EXT_RES, streamerServlet);

        if (enableHtml) {
            rendererMap.put(HtmlRendererServlet.EXT_HTML,
                    getDefaultRendererServlet(HtmlRendererServlet.EXT_HTML));
        }

        if (enableTxt) {
            rendererMap.put(PlainTextRendererServlet.EXT_TXT,
                    getDefaultRendererServlet(PlainTextRendererServlet.EXT_TXT));
        }

        if (enableJson) {
            rendererMap.put(JsonRendererServlet.EXT_JSON,
                    getDefaultRendererServlet(JsonRendererServlet.EXT_JSON));
        }

        if (enableXml) {
            rendererMap.put(XMLRendererServlet.EXT_XML,
                    getDefaultRendererServlet(XMLRendererServlet.EXT_XML));
        }


        // check additional aliases
        if (this.aliases != null) {
            for (final String m : aliases) {
                final int pos = m.indexOf(':');
                if (pos != -1) {
                    final String type = m.substring(0, pos);
                    Servlet servlet = rendererMap.get(type);
                    if ( servlet == null ) {
                        servlet = getDefaultRendererServlet(type);
                    }
                    if (servlet != null) {
                        final String extensions = m.substring(pos + 1);
                        final StringTokenizer st = new StringTokenizer(
                            extensions, ",");
                        while (st.hasMoreTokens()) {
                            final String ext = st.nextToken();
                            rendererMap.put(ext, servlet);
                        }
                    } else {
                        logger.warn("Unable to enable renderer alias(es) for {} - type not supported", m);
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
                request.getResource().getPath(), "No resource found");
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
                "No renderer for extension " + ext);
            // if this is an included request, sendError() would fail
            // as the response is already committed, in this case we just
            // do nothing (but log an error message)
            if (response.isCommitted()
                || request.getAttribute(SlingConstants.ATTR_REQUEST_SERVLET) != null) {
                logger.error(
                    "No renderer for extension {}, cannot render resource {}",
                    ext, request.getResource());
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }

        request.getRequestProgressTracker().log(
            "Using " + rendererServlet.getClass().getName()
                + " to render for extension=" + ext);
        rendererServlet.service(request, response);
    }

    @Override
    protected void doHead(SlingHttpServletRequest request,
                          SlingHttpServletResponse response) throws ServletException,
            IOException {

        response = new HeadServletResponse(response);
        doGet(request, response);
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
}
