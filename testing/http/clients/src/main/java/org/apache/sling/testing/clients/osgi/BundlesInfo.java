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

package org.apache.sling.testing.clients.osgi;

import org.apache.sling.testing.clients.ClientException;
import org.codehaus.jackson.JsonNode;

import java.util.Iterator;

/**
 * A simple Wrapper around the returned JSON when requesting the status of /system/console/bundles
 */
public class BundlesInfo {

    private JsonNode root = null;

    private JsonNode status = null;

    /**
     * The only constructor.
     * 
     * @param root the root JSON node of the bundles info.
     * @throws ClientException if the json does not contain the proper info
     */
    public BundlesInfo(JsonNode root) throws ClientException {
        this.root = root;
        // some simple sanity checks
        if(root.get("s") == null)
            throw new ClientException("No Status Info returned!");
        if(root.get("s").size() != 5)
            throw new ClientException("Wrong number of status numbers listed!");
        status = root.get("s");
    }

    /**
     * @return the status message of the bundle context
     * @throws ClientException if the request cannot be completed
     */
    public String getStatusMessage() throws ClientException {
        if(root.get("status") == null)
            throw new ClientException("No Status message returned!");
        return root.get("status").getValueAsText();
    }

    /**
     * @return total number of bundles.
     */
    public int getTotalNumOfBundles() {
        return Integer.parseInt(status.get(0).getValueAsText());
    }

    /**
     * Returns number of bundles that are in specified state
     *
     * @param status the requested status
     * @return the number of bundles
     */
    public int getNumBundlesByStatus(Bundle.Status status) {
        int index = -1;
        switch(status) {
        case ACTIVE:
            index = 1;
            break;
        case FRAGMENT:
            index = 2;
            break;
        case RESOLVED:
            index = 3;
            break;
        case INSTALLED:
            index = 4;
            break;
        }
        return Integer.parseInt(this.status.get(index).getValueAsText());
    }

    /**
     * Return bundle info for a bundle with persistence identifier {@code pid}
     *
     * @param id the id of the bundle
     * @return the BundleInfo
     * @throws ClientException if the info could not be retrieved
     */
    public BundleInfo forId(String id) throws ClientException {
        JsonNode bundle = findBy("id", id);
        return (bundle != null) ? new BundleInfo(bundle) : null;
    }

    /**
     * Return bundle info for a bundle with name {@code name}
     *
     * @param name the name of the requested bundle
     * @return the info, or {@code null} if the bundle is not found
     * @throws ClientException if the info cannot be retrieved
     */
    public BundleInfo forName(String name) throws ClientException {
        JsonNode bundle = findBy("name", name);
        return (bundle != null) ? new BundleInfo(bundle) : null;
    }

    /**
     * Return bundle info for a bundle with symbolic name {@code name}
     *
     * @param name the symbolic name of the requested bundle
     * @return the info, or {@code null} if the bundle is not found
     * @throws ClientException if the info cannot be retrieved
     */
    public BundleInfo forSymbolicName(String name) throws ClientException {
        JsonNode bundle = findBy("symbolicName", name);
        return (bundle != null) ? new BundleInfo(bundle) : null;
    }

    private JsonNode findBy(String key, String value) {
        Iterator<JsonNode> nodes = root.get("data").getElements();
        while(nodes.hasNext()) {
            JsonNode node = nodes.next();
            if(node.get(key) != null) {
                if(node.get(key).isValueNode()) {
                	String valueNode=node.get(key).getTextValue();
                	if (valueNode.equals(value)){
                		return node;
                	}
                }
            }
        }
        return null;
    }

}