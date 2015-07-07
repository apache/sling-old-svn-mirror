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
package org.apache.sling.scripting.thymeleaf.internal.processor.attr;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.scripting.core.servlet.CaptureResponseWrapper;
import org.apache.sling.scripting.thymeleaf.internal.SlingWebContext;
import org.apache.sling.scripting.thymeleaf.internal.dom.NodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.Arguments;
import org.thymeleaf.Configuration;
import org.thymeleaf.context.IContext;
import org.thymeleaf.dom.Element;
import org.thymeleaf.dom.Macro;
import org.thymeleaf.processor.ProcessorResult;
import org.thymeleaf.processor.attr.AbstractAttrProcessor;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;

public class SlingIncludeAttrProcessor extends AbstractAttrProcessor {

    public static final int ATTR_PRECEDENCE = 100;

    public static final String ATTR_NAME = "include";

    private final Logger logger = LoggerFactory.getLogger(SlingIncludeAttrProcessor.class);

    public SlingIncludeAttrProcessor() {
        super(ATTR_NAME);
    }

    @Override
    public int getPrecedence() {
        return ATTR_PRECEDENCE;
    }

    @Override
    protected ProcessorResult processAttribute(final Arguments arguments, final Element element, final String attributeName) {
        final IContext context = arguments.getTemplateProcessingParameters().getContext();
        if (context instanceof SlingWebContext) {
            final SlingWebContext slingWebContext = (SlingWebContext) context;
            final SlingHttpServletRequest slingHttpServletRequest = slingWebContext.getHttpServletRequest();
            final SlingHttpServletResponse slingHttpServletResponse = slingWebContext.getHttpServletResponse();
            // resource or path to include
            final Configuration configuration = arguments.getConfiguration();
            final String attributeValue = element.getAttributeValue(attributeName);
            final IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);
            final IStandardExpression expression = parser.parseExpression(configuration, arguments, attributeValue);
            final Object include = expression.execute(configuration, arguments);
            String path = null;
            if (include instanceof String) {
                path = (String) include;
            }
            Resource resource = null;
            if (include instanceof Resource) {
                resource = (Resource) include;
            }
            // request dispatcher options
            final RequestDispatcherOptions requestDispatcherOptions = prepareRequestDispatcherOptions(element);
            // dispatch
            final String content = dispatch(resource, path, slingHttpServletRequest, slingHttpServletResponse, requestDispatcherOptions);
            // cleanup
            element.removeAttribute(attributeName);
            element.clearChildren();
            // add output
            final Macro macro = new Macro(content);
            element.addChild(macro);
            final Boolean unwrap = NodeUtil.getNodeProperty(element, SlingUnwrapAttrProcessor.NODE_PROPERTY_NAME, Boolean.class);
            if (unwrap != null && unwrap) {
                element.getParent().extractChild(element);
            }
        } else {
            throw new RuntimeException("Context is not an instance of SlingWebContext, unable to process include attribute");
        }
        return ProcessorResult.OK;
    }

    protected RequestDispatcherOptions prepareRequestDispatcherOptions(final Element element) {
        final String resourceType = NodeUtil.getNodeProperty(element, SlingResourceTypeAttrProcessor.NODE_PROPERTY_NAME, String.class);
        final String replaceSelectors = NodeUtil.getNodeProperty(element, SlingReplaceSelectorsAttrProcessor.NODE_PROPERTY_NAME, String.class);
        final String addSelectors = NodeUtil.getNodeProperty(element, SlingAddSelectorsAttrProcessor.NODE_PROPERTY_NAME, String.class);
        final String replaceSuffix = NodeUtil.getNodeProperty(element, SlingReplaceSuffixAttrProcessor.NODE_PROPERTY_NAME, String.class);
        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setForceResourceType(resourceType);
        options.setReplaceSelectors(replaceSelectors);
        options.setAddSelectors(addSelectors);
        options.setReplaceSuffix(replaceSuffix);
        return options;
    }

    /**
     * @see "org.apache.sling.scripting.jsp.taglib.IncludeTagHandler"
     *
     * @param resource the resource to include
     * @param path the path to include
     * @param slingHttpServletRequest the current request
     * @param slingHttpServletResponse the current response
     * @param requestDispatcherOptions the options for the request dispatcher
     * @return the character response from the include call to request dispatcher
     */
    protected String dispatch(Resource resource, String path, final SlingHttpServletRequest slingHttpServletRequest, final SlingHttpServletResponse slingHttpServletResponse, final RequestDispatcherOptions requestDispatcherOptions) {

        // ensure the path (if set) is absolute and normalized
        if (path != null) {
            if (!path.startsWith("/")) {
                path = slingHttpServletRequest.getResource().getPath() + "/" + path;
            }
            path = ResourceUtil.normalize(path);
        }

        // check the resource
        if (resource == null) {
            if (path == null) {
                // neither resource nor path is defined, use current resource
                resource = slingHttpServletRequest.getResource();
            } else {
                // check whether the path (would) resolve, else SyntheticRes.
                final String resourceType = requestDispatcherOptions.getForceResourceType();
                final Resource tmp = slingHttpServletRequest.getResourceResolver().resolve(path);
                if (tmp == null && resourceType != null) {
                    resource = new SyntheticResource(slingHttpServletRequest.getResourceResolver(), path, resourceType); // TODO DispatcherSyntheticResource?
                    // remove resource type overwrite as synthetic resource is correctly typed as requested
                    requestDispatcherOptions.remove(RequestDispatcherOptions.OPT_FORCE_RESOURCE_TYPE);
                }
            }
        }

        try {
            // create a dispatcher for the resource or path
            final RequestDispatcher dispatcher;
            if (resource != null) {
                dispatcher = slingHttpServletRequest.getRequestDispatcher(resource, requestDispatcherOptions);
            } else {
                dispatcher = slingHttpServletRequest.getRequestDispatcher(path, requestDispatcherOptions);
            }

            if (dispatcher != null) {
                try {
                    final CaptureResponseWrapper wrapper = new CaptureResponseWrapper(slingHttpServletResponse);
                    dispatcher.include(slingHttpServletRequest, wrapper);
                    if (!wrapper.isBinaryResponse()) {
                        return wrapper.getCapturedCharacterResponse();
                    }
                } catch (ServletException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                logger.error("no request dispatcher: unable to include {}/'{}'", resource, path);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

}
