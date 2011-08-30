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
package org.apache.sling.samples.urlfilter.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Illustration of a technique for url filtering on suffixes, selectors, and/or
 * extensions
 */
@SlingFilter(scope = SlingFilterScope.REQUEST, order = Integer.MIN_VALUE)
public class UrlFilter implements Filter {

    static final String PN_ALLOWED_EXTENSION_PATTERN = "allowedExtensionPattern";

    static final String PN_ALLOWED_EXTENSIONS = "allowedExtensions";

    static final String PN_ALLOWED_SELECTOR_PATTERN = "allowedSelectorPattern";

    static final String PN_ALLOWED_SELECTORS = "allowedSelectors";

    static final String PN_ALLOWED_SUFFIX_PATTERN = "allowedSuffixPattern";

    static final String PN_ALLOWED_SUFFIXES = "allowedSuffixes";

    static final Collection<String> PROPERTY_NAMES = Arrays.asList(PN_ALLOWED_SUFFIXES, PN_ALLOWED_EXTENSIONS,
            PN_ALLOWED_SELECTORS, PN_ALLOWED_SUFFIX_PATTERN, PN_ALLOWED_SELECTOR_PATTERN, PN_ALLOWED_EXTENSION_PATTERN);

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void destroy() {
        // nothing to do
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        if (request instanceof SlingHttpServletRequest && response instanceof SlingHttpServletResponse) {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

            RequestPathInfo pathInfo = slingRequest.getRequestPathInfo();

            Resource definitionResource = findUrlFilterDefinitionResource(slingRequest.getResource(),
                    slingRequest.getResourceResolver());

            if (definitionResource != null) {
                logger.debug("found url filter definition resource at {}", definitionResource.getPath());
                ValueMap properties = definitionResource.adaptTo(ValueMap.class);
                if (properties != null) {
                    if (checkSelector(pathInfo, properties) && checkSuffix(pathInfo, properties)
                            && checkExtension(pathInfo, properties)) {
                        logger.debug("url filter definition resource at {} passed for request {}.",
                                definitionResource.getPath(), slingRequest.getRequestPathInfo());
                    } else {
                        logger.info("url filter definition resource at {} FAILED for request {}.",
                                definitionResource.getPath(), slingRequest.getRequestPathInfo());
                        slingResponse.sendError(403);
                        return;
                    }
                }
            }

        }

        chain.doFilter(request, response);

    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    boolean checkExtension(RequestPathInfo pathInfo, ValueMap properties) {
        return check(pathInfo.getExtension(), PN_ALLOWED_EXTENSIONS, PN_ALLOWED_EXTENSION_PATTERN, properties);
    }

    boolean checkSelector(RequestPathInfo pathInfo, ValueMap properties) {
        return check(pathInfo.getSelectorString(), PN_ALLOWED_SELECTORS, PN_ALLOWED_SELECTOR_PATTERN, properties);
    }

    boolean check(String value, String allowedArrayPropertyName, String allowedPatternPropertyName, ValueMap properties) {
        if (value == null) {
            // no value is always allowed
            return true;
        }
        String[] allowedValues = properties.get(allowedArrayPropertyName, String[].class);
        if (allowedValues != null) {
            if (allowedValues.length == 0) {
                logger.debug("{} was empty, therefore not allowing any value.", allowedArrayPropertyName);
                return false;
            } else if (!ArrayUtils.contains(allowedValues, value)) {
                logger.debug("{} did not contain our string {}. checking the pattern.", allowedArrayPropertyName, value);
                String allowedPattern = properties.get(allowedPatternPropertyName, String.class);
                if (allowedPattern == null || !Pattern.matches(allowedPattern, value)) {
                    logger.debug("allowedPattern ({}) did not match our string {}", allowedPattern, value);
                    return false;
                } else {
                    logger.debug("allowedPattern ({}) did match our string {}", allowedPattern, value);
                    return true;
                }
            } else {
                return true;
            }
        } else {
            String allowedPattern = properties.get(allowedPatternPropertyName, String.class);
            if (allowedPattern != null && !Pattern.matches(allowedPattern, value)) {
                logger.debug("allowedPattern ({}) did not match our string {}", allowedPattern, value);
                return false;
            } else {
                return true;
            }
        }
    }

    boolean checkSuffix(RequestPathInfo pathInfo, ValueMap properties) {
        return check(pathInfo.getSuffix(), PN_ALLOWED_SUFFIXES, PN_ALLOWED_SUFFIX_PATTERN, properties);
    }

    Resource findUrlFilterDefinitionResource(Resource resource, ResourceResolver resolver) {
        if (resource == null) {
            return null;
        }
        
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource != null) {
            resource = contentResource;
        }

        String resourceType = resource.getResourceType();

        Resource definitionResource = findUrlFilterDefinitionResource(resourceType, resolver);
        if (definitionResource == null) {
            return findUrlFilterDefinitionResource(resource.getResourceSuperType(), resolver);
        } else {
            return definitionResource;
        }
    }

    private Resource findUrlFilterDefinitionResource(String resourceType, ResourceResolver resolver) {
        if (resourceType == null) {
            return null;
        }
        Resource typeResource = resolver.getResource(resourceType);
        if (typeResource == null) {
            return null;
        }

        ValueMap properties = typeResource.adaptTo(ValueMap.class);
        if (properties == null) {
            return null;
        }

        // Collections.disjoint returns true if the collections
        // have nothing in common, so when it is false, use the current resource
        if (!Collections.disjoint(properties.keySet(), PROPERTY_NAMES)) {
            return typeResource;
        } else {
            // otherwise, look at the resource type resource's super type
            return findUrlFilterDefinitionResource(typeResource.getResourceSuperType(), resolver);
        }

    }
}
