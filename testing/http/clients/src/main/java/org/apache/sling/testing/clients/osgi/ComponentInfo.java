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

public class ComponentInfo {

    private JsonNode component;

    public ComponentInfo(JsonNode root) throws ClientException {
        if(root.get("id") != null) {
            if(root.get("id") == null) {
                throw new ClientException("No Component Info returned");
            }
            component = root;
        } else {
            if(root.get("data") == null && root.get("data").size() < 1) {
                throw new ClientException("No Component Info returned");
            }
            component = root.get("data").get(0);
        }
    }

    /**
     * @return the component identifier
     */
    public int getId() {
        return component.get("id").getIntValue();
    }

    /**
     * @return the component name
     */
    public String getName() {
        return component.get("name").getTextValue();
    }

    /**
     * @return the component status
     */
    public Component.Status getStatus() {
        return Component.Status.value(component.get("state").getTextValue());
    }

    /**
     * @return the component persistent identifier
     */
    public String getPid() {
        return component.get("pid").getTextValue();
    }

}
