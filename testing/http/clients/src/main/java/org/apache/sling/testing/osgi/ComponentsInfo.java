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

import java.util.Iterator;

/**
 * Thin wrapper around the list of components
 */
public class ComponentsInfo {

    private JsonNode root = null;

    /**
     * The only constructor.
     * 
     * @param rootNode the root JSON node of the components info.
     * @throws ClientException if the info cannot be retrieved
     */
    public ComponentsInfo(JsonNode rootNode) throws ClientException {
        this.root = rootNode;
    }

    /**
     * @return the number of installed components
     * @throws ClientException if the info cannot be retrieved
     */
    public int getNumberOfInstalledComponents() throws ClientException {
        if(root.get("status") == null)
            throw new ClientException("Number of installed Components not defined!");
        return Integer.parseInt(root.get("status").getValueAsText());
    }

    /**
     * @param id the id of the component
     * @return the ComponentInfo for a component with the identifier {@code id}
     * @throws ClientException if the info cannot be retrieved
     */
    public ComponentInfo forId(String id) throws ClientException {
        JsonNode component = findBy("id", id);
        return (component != null) ? new ComponentInfo(component) : null;
    }

    /**
     * @param name the name of the component
     * @return the ComponentInfo for a component with the name {@code name}
     * @throws ClientException if the info cannot be retrieved
     */
    public ComponentInfo forName(String name) throws ClientException {
        JsonNode component = findBy("name", name);
        return (component != null) ? new ComponentInfo(component) : null;
    }

    /**
     * @param pid the pid of the component
     * @return the ComponentInfo for a component with the pid {@code pid}
     * @throws ClientException if the info cannot be retrieved
     */
    public ComponentInfo forPid(String pid) throws ClientException {
        JsonNode component = findBy("pid", pid);
        return (component != null) ? new ComponentInfo(component) : null;
    }

    private JsonNode findBy(String key, String value) {
        Iterator<JsonNode> nodes = root.get("data").getElements();
        while(nodes.hasNext()) {
            JsonNode node = nodes.next();
            if(node.get(key) != null) {
                if(node.get(key).isValueNode()) {
                    return node;
                }
            }
        }
        return null;
    }

}