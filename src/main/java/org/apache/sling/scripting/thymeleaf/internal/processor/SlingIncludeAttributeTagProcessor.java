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
import org.apache.sling.scripting.core.servlet.CaptureResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ITemplateProcessingContext;
import org.thymeleaf.context.IVariablesMap;
import org.thymeleaf.context.IWebVariablesMap;
import org.thymeleaf.dialect.IProcessorDialect;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;

public class SlingIncludeAttributeTagProcessor extends SlingHtmlAttributeTagProcessor {

    public static final int ATTRIBUTE_PRECEDENCE = 100;

    public static final String ATTRIBUTE_NAME = "include";

    private final Logger logger = LoggerFactory.getLogger(SlingIncludeAttributeTagProcessor.class);

    public SlingIncludeAttributeTagProcessor(final IProcessorDialect processorDialect, final String dialectPrefix) {
        super(processorDialect, dialectPrefix, ATTRIBUTE_NAME, ATTRIBUTE_PRECEDENCE, true);
    }

    @Override
    protected void doProcess(final ITemplateProcessingContext processingContext, final IProcessableElementTag tag, final AttributeName attributeName, final String attributeValue, final String tagTemplateName, final int tagLine, final int tagCol, final IElementTagStructureHandler elementTagStructureHandler) {
        // final IContext context = arguments.getTemplateProcessingParameters().getContext();
        // if (context instanceof SlingWebContext) {
        try {
            final IWebVariablesMap webVariablesMap = (IWebVariablesMap) processingContext.getVariables();
            final SlingHttpServletRequest slingHttpServletRequest = (SlingHttpServletRequest) webVariablesMap.getRequest();
            final SlingHttpServletResponse slingHttpServletResponse = (SlingHttpServletResponse) webVariablesMap.getResponse();

            final IEngineConfiguration configuration = processingContext.getConfiguration();
            final IStandardExpressionParser expressionParser = StandardExpressions.getExpressionParser(configuration);
            final IStandardExpression expression = expressionParser.parseExpression(processingContext, attributeValue);
            final Object include = expression.execute(processingContext);

            String path = null;
            if (include instanceof String) {
                path = (String) include;
            }
            Resource resource = null;
            if (include instanceof Resource) {
                resource = (Resource) include;
            }
            // request dispatcher options
            final RequestDispatcherOptions requestDispatcherOptions = prepareRequestDispatcherOptions(webVariablesMap);
            // dispatch
            final String content = dispatch(resource, path, slingHttpServletRequest, slingHttpServletResponse, requestDispatcherOptions);
            // cleanup
            tag.getAttributes().removeAttribute(attributeName);
            // element.clearChildren();
            // add output
            final Boolean unwrap = (Boolean) webVariablesMap.getVariable(SlingUnwrapAttributeTagProcessor.NODE_PROPERTY_NAME);
            if (unwrap != null && unwrap) {
                elementTagStructureHandler.replaceWith(content, false);
            } else {
                elementTagStructureHandler.setBody(content, false);
            }
        } catch (Exception e) {
            throw new RuntimeException("unable to process include attribute", e);
        }
    }

    protected RequestDispatcherOptions prepareRequestDispatcherOptions(final IVariablesMap variablesMap) {
        final String resourceType = (String) variablesMap.getVariable(SlingResourceTypeAttributeTagProcessor.NODE_PROPERTY_NAME);
        final String replaceSelectors = (String) variablesMap.getVariable(SlingReplaceSelectorsAttributeTagProcessor.NODE_PROPERTY_NAME);
        final String addSelectors = (String) variablesMap.getVariable(SlingAddSelectorsAttributeProcessor.NODE_PROPERTY_NAME);
        final String replaceSuffix = (String) variablesMap.getVariable(SlingReplaceSuffixAttributeTagProcessor.NODE_PROPERTY_NAME);
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
