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
package org.apache.sling.scripting.thymeleaf.internal.processor;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.core.servlet.CaptureResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

public class SlingIncludeAttributeTagProcessor extends AbstractAttributeTagProcessor {

    public static final int ATTRIBUTE_PRECEDENCE = 100;

    public static final String ATTRIBUTE_NAME = "include";

    public static final String ADD_SELECTORS_ATTRIBUTE_NAME = "addSelectors";

    public static final String REPLACE_SELECTORS_ATTRIBUTE_NAME = "replaceSelectors";

    public static final String REPLACE_SUFFIX_ATTRIBUTE_NAME = "replaceSuffix";

    public static final String RESOURCE_TYPE_ATTRIBUTE_NAME = "resourceType";

    public static final String UNWRAP_ATTRIBUTE_NAME = "unwrap";

    private final Logger logger = LoggerFactory.getLogger(SlingIncludeAttributeTagProcessor.class);

    public SlingIncludeAttributeTagProcessor(final String dialectPrefix) {
        super(TemplateMode.HTML, dialectPrefix, null, true, ATTRIBUTE_NAME, true, ATTRIBUTE_PRECEDENCE, true);
    }

    @Override
    protected void doProcess(final ITemplateContext templateContext, final IProcessableElementTag processableElementTag, final AttributeName attributeName, final String attributeValue, final IElementTagStructureHandler elementTagStructureHandler) {
        try {
            final SlingHttpServletRequest slingHttpServletRequest = (SlingHttpServletRequest) templateContext.getVariable(SlingBindings.REQUEST);
            final SlingHttpServletResponse slingHttpServletResponse = (SlingHttpServletResponse) templateContext.getVariable(SlingBindings.RESPONSE);

            final IEngineConfiguration configuration = templateContext.getConfiguration();
            final IStandardExpressionParser expressionParser = StandardExpressions.getExpressionParser(configuration);
            final IStandardExpression expression = expressionParser.parseExpression(templateContext, attributeValue);
            final Object include = expression.execute(templateContext);

            String path = null;
            if (include instanceof String) {
                path = (String) include;
            }
            Resource resource = null;
            if (include instanceof Resource) {
                resource = (Resource) include;
            }
            // request dispatcher options
            final RequestDispatcherOptions requestDispatcherOptions = prepareRequestDispatcherOptions(expressionParser, templateContext, processableElementTag, elementTagStructureHandler);
            // dispatch
            final String content = dispatch(resource, path, slingHttpServletRequest, slingHttpServletResponse, requestDispatcherOptions);
            // add output
            final Boolean unwrap = (Boolean) parseAttribute(expressionParser, templateContext, processableElementTag, elementTagStructureHandler, UNWRAP_ATTRIBUTE_NAME);
            if (unwrap != null && unwrap) {
                elementTagStructureHandler.replaceWith(content, false);
            } else {
                elementTagStructureHandler.setBody(content, false);
            }
        } catch (Exception e) {
            throw new RuntimeException("unable to process include attribute", e);
        }
    }

    protected Object parseAttribute(final IStandardExpressionParser expressionParser, final ITemplateContext templateContext, final IProcessableElementTag processableElementTag, final IElementTagStructureHandler elementTagStructureHandler, final String name) {
        final String value = processableElementTag.getAttributeValue(getDialectPrefix(), name);
        Object result = null;
        if (value != null) {
            final IStandardExpression expression = expressionParser.parseExpression(templateContext, value);
            result = expression.execute(templateContext);
        }
        elementTagStructureHandler.removeAttribute(getDialectPrefix(), name);
        return result;
    }

    protected RequestDispatcherOptions prepareRequestDispatcherOptions(final IStandardExpressionParser expressionParser, final ITemplateContext templateContext, final IProcessableElementTag processableElementTag, final IElementTagStructureHandler elementTagStructureHandler) {
        final String resourceType = (String) parseAttribute(expressionParser, templateContext, processableElementTag, elementTagStructureHandler, RESOURCE_TYPE_ATTRIBUTE_NAME);
        final String replaceSelectors = (String) parseAttribute(expressionParser, templateContext, processableElementTag, elementTagStructureHandler, REPLACE_SELECTORS_ATTRIBUTE_NAME);
        final String addSelectors = (String) parseAttribute(expressionParser, templateContext, processableElementTag, elementTagStructureHandler, ADD_SELECTORS_ATTRIBUTE_NAME);
        final String replaceSuffix = (String) parseAttribute(expressionParser, templateContext, processableElementTag, elementTagStructureHandler, REPLACE_SUFFIX_ATTRIBUTE_NAME);

        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setForceResourceType(resourceType);
        options.setReplaceSelectors(replaceSelectors);
        options.setAddSelectors(addSelectors);
        options.setReplaceSuffix(replaceSuffix);
        return options;
    }

    /**
     * @param resource the resource to include
     * @param path the path to include
     * @param slingHttpServletRequest the current request
     * @param slingHttpServletResponse the current response
     * @param requestDispatcherOptions the options for the request dispatcher
     * @return the character response from the include call to request dispatcher
     * @see "org.apache.sling.scripting.jsp.taglib.IncludeTagHandler"
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
