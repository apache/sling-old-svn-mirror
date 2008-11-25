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
package org.apache.sling.jcr.resource.internal.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.internal.JcrResourceResolver2;

/**
 * The <code>MapEntry</code> class represents a mapping entry in the mapping
 * configuration tree at <code>/etc/map</code>.
 * <p>
 * @see http://cwiki.apache.org/SLING/flexible-resource-resolution.html
 */
public class MapEntry {

    private final Pattern urlPattern;

    private final String redirect;

    private final boolean isInternal;

    public static MapEntry create(String url, Resource resource) {
        ValueMap props = resource.adaptTo(ValueMap.class);
        if (props != null) {
            String redirect = props.get(
                JcrResourceResolver2.PROP_REDIRECT_EXTERNAL, String.class);
            if (redirect != null) {
                return new MapEntry(url, redirect, false);
            }
            
            redirect = props.get(
                JcrResourceResolver2.PROP_REDIRECT_INTERNAL, String.class);
            if (redirect != null) {
                return new MapEntry(url, redirect, true);
            }
        }

        return null;
    }

    public MapEntry(String url, String redirect, boolean isInternal) {
        this.urlPattern = Pattern.compile(url);
        this.redirect = redirect;
        this.isInternal = isInternal;
    }

    public Matcher getMatcher(String value) {
        return urlPattern.matcher(value);
    }
    
    // Returns the replacement or null if the value does not match
    public String replace(String value) {
        Matcher m = urlPattern.matcher(value);
        if (m.find()) {
            StringBuffer buf = new StringBuffer();
            m.appendReplacement(buf, getRedirect());
            m.appendTail(buf);
            return buf.toString();
        }
     
        return null;
    }

    public String getRedirect() {
        return redirect;
    }

    public boolean isInternal() {
        return isInternal;
    }
}
