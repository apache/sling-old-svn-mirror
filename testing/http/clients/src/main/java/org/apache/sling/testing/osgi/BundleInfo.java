/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.sling.testing.osgi;

import org.apache.sling.testing.itframework.ClientException;
import org.codehaus.jackson.JsonNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BundleInfo {

    private JsonNode bundle;

    public BundleInfo(JsonNode root) throws ClientException {
        if(root.get("id") != null) {
            if(root.get("id") == null) {
                throw new ClientException("No Bundle Info returned");
            }
            bundle = root;
        } else {
            if(root.get("data") == null && root.get("data").size() < 1) {
                throw new ClientException("No Bundle Info returned");
            }
            bundle = root.get("data").get(0);
        }
    }

    /**
     * @return the bundle identifier
     */
    public int getId() {
        return bundle.get("id").getIntValue();
    }

    /**
     * @return the bundle name
     */
    public String getName() {
        return bundle.get("name").getTextValue();
    }

    /**
     * @return the bundle version
     */
    public String getVersion() {
        return bundle.get("version").getTextValue();
    }

    /**
     * Returns the indicator if the bundle is a fragment
     * 
     * @return {@code true} if bundle is a fragment, {@code false} otherwise.
     */
    public boolean isFragment() {
        return bundle.get("fragment").getBooleanValue();
    }

    /**
     * @return the bundle current state
     */
    public Bundle.Status getStatus() {
        return Bundle.Status.value(bundle.get("state").getTextValue());
    }

    /**
     * @return the bundle symbolic name
     */
    public String getSymbolicName() {
        return bundle.get("symbolicName").getTextValue();
    }

    /**
     * @return the category of the bundle
     */
    public String getCategory() {
        return bundle.get("category").getTextValue();
    }

    /**
     * Returns the value of a specific key in the bundle
     *
     * @param key the property to search
     * @return a specific bundle property
     */
    public String getProperty(String key) {
        Map<String, String> props = getProperties();
        return props.get(key);
    }

    /**
     * @return the bundle properties in a {@link Map}
     */
    public Map<String, String> getProperties() {
        JsonNode props = bundle.get("props");
        Map<String, String> entries = new HashMap<String, String>();

        if(props != null) {
            Iterator<JsonNode> it = props.getElements();
            while(it.hasNext()) {
                JsonNode n = it.next();
                entries.put(n.get("key").getTextValue(), n.get("value").getTextValue());
            }
        }
        return entries;
    }

}
