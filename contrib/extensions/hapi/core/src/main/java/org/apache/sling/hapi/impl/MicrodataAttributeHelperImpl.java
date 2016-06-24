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
package org.apache.sling.hapi.impl;

import org.apache.sling.hapi.HApiProperty;
import org.apache.sling.hapi.HApiType;
import org.apache.sling.hapi.MicrodataAttributeHelper;
import org.apache.sling.hapi.HApiException;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * Helper class for HTML microdata attributes
 */
public class MicrodataAttributeHelperImpl implements MicrodataAttributeHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MicrodataAttributeHelperImpl.class);
    private final ResourceResolver resolver;
    HApiType type;


    /**
     * Get a new microdata html attributes helper for the given HApiType object.
     * <p>Provides convenience methods to get the html attributes needed for instrumenting the markup with a Hypermedia API</p>
     * @param resolver
     * @param type
     */
    public MicrodataAttributeHelperImpl(ResourceResolver resolver, HApiType type) {
        this.resolver = resolver;
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public String itemtype() {
        return itemtypeMap().toString();
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> itemtypeMap() {
        Map<String, String> attrMap = new AttrMap(2);
        attrMap.put("itemtype", type.getUrl());
        attrMap.put("itemscope", String.valueOf(!type.getAllProperties().isEmpty()));
        
        return attrMap;
    }

    /**
     * {@inheritDoc}
     */
    public String itemprop(String propName) {
        return itemprop(propName, true);
    }

    /**
     * {@inheritDoc}
     */
    public String itemprop(String propName, boolean withType) {
        return itempropMap(propName, withType).toString();
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> itempropMap(String propName, boolean withType) {
        HApiProperty prop = this.type.getAllProperties().get(propName);
        if (null == prop) throw new HApiException("Property " + propName + " does not exist for type " + type.getPath());

        Map<String, String> attrMap = new AttrMap(3);
        attrMap.put("itemprop", propName);
        if (withType) {
            attrMap.putAll(new MicrodataAttributeHelperImpl(resolver, prop.getType()).itemtypeMap());
        }
        return attrMap;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Map<String, String>> allItemPropMap() {
        Map<String, Map<String, String>> m = new PropMap(type);
        for (String prop: type.getAllProperties().keySet()) {
            m.put(prop, itempropMap(prop, true));
        }
        return m;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> allPropTypesMap() {
        Map<String, HApiProperty> props = type.getAllProperties();
        Map<String, String> types = new HashMap<String, String>(props.size());
        for (String propName : props.keySet()) {
            types.put(propName, props.get(propName).getType().getPath());
        }
        return types;
    }


    /**
     * {@inheritDoc}
     */
    private class PropMap extends HashMap<String, Map<String, String>> {

        private final HApiType type;

        public PropMap(HApiType type) {
            super();
            this.type = type;
        }

        @Override
        public Map<String, String> get(Object key) {
            Map<String, String> val = super.get(key);
            if (null == val) {
                LOG.debug("type = {}", type);
                throw new HApiException("Property " + key + " does not exist for type " + type.getPath());
            }
            return val;
        }
    }

    /**
     * {@inheritDoc}
     */
    private class AttrMap extends HashMap<String, String> {

        public AttrMap(int i) {
            super(i);
        }

        public AttrMap() {
            super();
        }

        public AttrMap(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        @Override
        public String toString() {
            String norm = "";
            for (Map.Entry<String, String> entry : this.entrySet()) {
                norm += entry.getKey() + "=\"" + entry.getValue() + "\"" + " ";
            }
            return norm;
        }
    }
}

