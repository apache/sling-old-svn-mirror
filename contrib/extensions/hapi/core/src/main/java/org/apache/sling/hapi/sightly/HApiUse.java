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

import java.util.Map;

import javax.script.Bindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.hapi.HApiUtil;
import org.apache.sling.hapi.MicrodataAttributeHelper;
import org.apache.sling.scripting.sightly.pojo.Use;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sightly use class helper to provide the hypermedia API microdata attributes for the type configured throught the 'type' binding.
 * <p>The type can be a JCR path or a fully qualified domain name like in
 * {@link HApiUtil#getTypeNode(org.apache.sling.api.resource.ResourceResolver, String)}</p>
 * <p>The convenience get methods are meant to be used in the 'data-sly-attribute' in the sightly script.</p>
 */
public class HApiUse implements Use {
    private static final Logger LOG = LoggerFactory.getLogger(HApiUse.class);

    private HApiUtil hapi;
    private MicrodataAttributeHelper helper;
    private SlingHttpServletRequest request;
    private SlingScriptHelper sling;
    private ResourceResolver resourceResolver;
    private String typeId;
    private Map<String, String> itemTypeAttr;
    private Map<String, Map<String, String>> itemPropAttrs;
    private Map<String, String> itemPropTypes;

    /**
     * {@inheritDoc}
     * @param bindings
     */
    @Override
    public void init(Bindings bindings) {
        request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
        sling = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
        resourceResolver = request.getResourceResolver();
        typeId = (String) bindings.get("type");
        LOG.debug("init type: {}", typeId);

        try {
            activate();
        } catch (Exception e) {
            LOG.error("Failed to activate Use class", e);
        }
    }

    /**
     * Initializes the helper and the attribute maps for the given type though the bindings
     * @throws Exception
     */
    public void activate() throws Exception {
        hapi = sling.getService(HApiUtil.class);
        helper = hapi.getHelper(resourceResolver, typeId);
        itemTypeAttr = helper.itemtypeMap();
        itemPropAttrs = helper.allItemPropMap();
        itemPropTypes = helper.allPropTypesMap();
    }

    /**
     * Get the itemtype html attributes map for the type
     * @return
     */
    public Map<String, String> getItemtype() {
        LOG.debug("itemtype attrs: {}", itemTypeAttr);
        return itemTypeAttr;
    }

    /**
     * Get the itemprop attributes map for the type, for each property.
     * The key is the property name and the value is a map of html attributes for the property
     * @return
     */
    public Map<String, Map<String, String>> getItemprop() {
        LOG.debug("itemprop attrs: {}", itemPropAttrs);
        return itemPropAttrs;
    }

    /**
     * Get a map of the type for each property name of the type
     * The key is the property name and the value is the type path in JCR
     * @return
     */
    public Map<String, String> getProptype() {
        LOG.debug("property type attrs: {}", itemPropTypes);
        return itemPropTypes;
    }
}
