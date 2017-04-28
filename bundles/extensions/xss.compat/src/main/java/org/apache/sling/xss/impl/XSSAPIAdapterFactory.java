/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or
 * more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************/
package org.apache.sling.xss.impl;

import org.apache.sling.xss.XSSAPI;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Adapter factory that adapts a {@link ResourceResolver} to a resourceResolver-specific
 * {@link XSSAPI} service.
 */
@Component(metatype = false)
@Service(AdapterFactory.class)
@Properties({
        @Property(name = "service.description", value = "Adapter for the XSSAPI service.")
})
@SuppressWarnings("unused")
public class XSSAPIAdapterFactory implements AdapterFactory {
    private static final Logger log = LoggerFactory.getLogger(XSSAPIAdapterFactory.class);
    private static final Class<XSSAPI> XSSAPI_CLASS = XSSAPI.class;
    private static final Class<ResourceResolver> RESOURCE_RESOLVER_CLASS = ResourceResolver.class;
    private static final Class<SlingHttpServletRequest> SLING_REQUEST_CLASS = SlingHttpServletRequest.class;

    @Reference
    XSSAPI xssApi;

    @Property(name = "adapters")
    public static final String[] ADAPTER_CLASSES = {
            XSSAPI_CLASS.getName()
    };

    @Property(name = "adaptables")
    public static final String[] ADAPTABLE_CLASSES = {
            RESOURCE_RESOLVER_CLASS.getName(),
            SLING_REQUEST_CLASS.getName()
    };

    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        if (adaptable instanceof ResourceResolver) {
            return getAdapter((ResourceResolver) adaptable, type);
        } else if (adaptable instanceof SlingHttpServletRequest) {
            return getAdapter((SlingHttpServletRequest) adaptable, type);
        } else {
            log.warn("Unable to handle adaptable {}", adaptable.getClass().getName());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <AdapterType> AdapterType getAdapter(ResourceResolver resourceResolver, Class<AdapterType> type) {
        if (resourceResolver != null) {
            if (type == XSSAPI.class) {
                return (AdapterType) xssApi.getResourceResolverSpecificAPI(resourceResolver);
            }
        }
        log.debug("Unable to adapt resourceResolver to type {}", type.getName());
        return null;
    }

    @SuppressWarnings("unchecked")
    private <AdapterType> AdapterType getAdapter(SlingHttpServletRequest request, Class<AdapterType> type) {
        if (request != null) {
            if (type == XSSAPI.class) {
                return (AdapterType) xssApi.getRequestSpecificAPI(request);
            }
        }
        log.debug("Unable to adapt resourceResolver to type {}", type.getName());
        return null;
    }
}
