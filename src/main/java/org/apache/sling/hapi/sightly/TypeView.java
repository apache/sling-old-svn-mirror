/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.hapi.sightly;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.hapi.HApiProperty;
import org.apache.sling.hapi.HApiType;
import org.apache.sling.hapi.HApiUtil;
import org.apache.sling.scripting.sightly.pojo.Use;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.util.*;

public class TypeView implements Use {
    private static final Logger LOG = LoggerFactory.getLogger(TypeView.class);

    private HApiUtil hapi;
    private HApiType me;
    private HApiType parent;

    private String description;
    private SlingHttpServletRequest request;
    private SlingScriptHelper sling;
    private Resource resource;
    private ResourceResolver resourceResolver;

    public void init(Bindings bindings) {
        request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
        sling =   (SlingScriptHelper) bindings.get(SlingBindings.SLING);
        resource = (Resource)bindings.get(SlingBindings.RESOURCE);
        resourceResolver = request.getResourceResolver();

        try {
            activate();
        } catch (Exception e) {
            LOG.error("Failed to activate Use class", e);
        }
    }

    public void activate() throws Exception {
        hapi = sling.getService(HApiUtil.class);
        me = hapi.fromPath(resourceResolver, resource.getPath());
        LOG.debug("me: {}  resource: {}", me, resource.getPath());
        description = me.getDescription();
        parent = me.getParent();
    }

    public String getTitle() {
        return me.getFqdn();
    }

    public String getDescription() {
        return description;
    }

    public String getParentUrl() {
        if (null != parent) {
            return parent.getUrl();
        } else {
            return null;
        }
    }

    public String getParentFqdn() {
        if (null != parent) {
            return parent.getFqdn();
        } else {
            return null;
        }
    }

    public List<String> getParameters() {
        return me.getParameters();
    }

    public List<HApiProperty> getProps() {
        List<HApiProperty> props = new ArrayList<HApiProperty>(me.getAllProperties().values());
        LOG.debug("props: ", props);
        return props;
    }

    public boolean getHasProps() {
        return getProps().size() > 0;
    }
}
