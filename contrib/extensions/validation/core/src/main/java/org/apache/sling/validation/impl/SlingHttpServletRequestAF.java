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
package org.apache.sling.validation.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service(AdapterFactory.class)
@Properties({
    @Property(name = "service.description", value = "Sling HTTP Servlet Request Adaptor Factory")
})
public class SlingHttpServletRequestAF implements AdapterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SlingHttpServletRequestAF.class);

    private static final Class<ValueMap> VALUE_MAP_CLASS = ValueMap.class;
    private static final Class<SlingHttpServletRequest> SLING_HTTP_SERVLET_REQUEST_CLASS = SlingHttpServletRequest.class;

    @Property(name = ADAPTABLE_CLASSES)
    static final String[] ADAPTABLES = { SLING_HTTP_SERVLET_REQUEST_CLASS.getName() };

    @Property(name = ADAPTER_CLASSES)
    static final String[] ADAPTERS = { VALUE_MAP_CLASS.getName() };

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        AdapterType adapter = null;
        if (adaptable instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest request = (SlingHttpServletRequest) adaptable;
            if (type == VALUE_MAP_CLASS) {
                adapter = (AdapterType) new ValueMapDecorator(request.getParameterMap());
            } else {
                LOG.warn("Cannot handle adapter {}", type.getName());
            }
        } else {
            LOG.warn("Cannot handle adaptable {}", adaptable.getClass().getName());
        }
        return adapter;
    }

}
