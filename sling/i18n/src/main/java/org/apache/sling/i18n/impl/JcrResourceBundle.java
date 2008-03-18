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
package org.apache.sling.i18n.impl;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jcr.query.Query;

import org.apache.sling.api.resource.ResourceResolver;

public class JcrResourceBundle extends ResourceBundle {

    static final String PROP_KEY = "sling:key";

    private static final String PROP_VALUE = "sling:message";

    private static final String QUERY_BASE = "//element(*,mix:language)[@jcr:language='%s']/*";

    private static final String QUERY_LOAD_FULLY = QUERY_BASE + "/(@"
        + PROP_KEY + "|@" + PROP_VALUE + ")";

    private static final String QUERY_LOAD_RESOURCE = QUERY_BASE + "[@"
        + PROP_KEY + "='%s']/@" + PROP_VALUE;

    private final ResourceResolver resourceResolver;

    private Map<String, Object> resources;

    private boolean fullyLoaded;

    protected final Locale locale;

    JcrResourceBundle(Locale locale, ResourceResolver resourceResolver) {
        this.locale = locale;
        this.resourceResolver = resourceResolver;
        this.resources = new HashMap<String, Object>();
        this.fullyLoaded = false;
    }

    @Override
    protected void setParent(ResourceBundle parent) {
        super.setParent(parent);
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public Enumeration<String> getKeys() {
        // ensure all keys are loaded
        loadFully();

        Enumeration<String> parentKeys = (parent != null)
                ? parent.getKeys()
                : null;
        return new ResourceBundleEnumeration(resources.keySet(), parentKeys);
    }

    @Override
    protected Object handleGetObject(String key) {
        Object value = resources.get(key);
        if (value == null && !fullyLoaded) {
            value = loadResource(key);
            if (value != null) {
                resources.put(key, value);
            }
        }
        return value;
    }

    private void loadFully() {
        if (!fullyLoaded) {
            Iterator<Map<String, Object>> bundles = resourceResolver.queryResources(
                getFullLoadQuery(), Query.XPATH);
            while (bundles.hasNext()) {
                Map<String, Object> resource = bundles.next();
                Object key = resource.get(PROP_KEY);
                if (key instanceof String && !resources.containsKey(key)) {
                    Object value = resource.get(PROP_VALUE);
                    if (value != null) {
                        resources.put((String) key, value);
                    }
                }
            }
        }
    }

    private Object loadResource(String key) {
        // query for the resource
        Iterator<Map<String, Object>> bundles = resourceResolver.queryResources(
            getResourceQuery(key), Query.XPATH);
        if (bundles.hasNext()) {
            Map<String, Object> resource = bundles.next();
            return resource.get(PROP_VALUE);
        }

        return null;
    }

    private String getFullLoadQuery() {
        return String.format(QUERY_LOAD_FULLY, getLocale());
    }

    private String getResourceQuery(String key) {
        return String.format(QUERY_LOAD_RESOURCE, getLocale(), key);
    }

}
