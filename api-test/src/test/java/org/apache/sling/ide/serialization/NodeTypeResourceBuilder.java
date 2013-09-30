/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.serialization;

import org.apache.sling.ide.transport.ResourceProxy;

public class NodeTypeResourceBuilder {

    public static NodeTypeResourceBuilder newBuilder(ResourceProxy parent, String name) {

        String path;

        if (parent.getPath().endsWith("/")) {
            path = parent.getPath() + name;
        } else {
            path = parent.getPath() + "/" + name;
        }

        ResourceProxy resourceProxy = new ResourceProxy(path);
        // set defaults
        resourceProxy.addProperty("jcr:nodeTypeName", name);
        resourceProxy.addProperty("jcr:primaryType", "nt:nodeType");
        resourceProxy.addProperty("jcr:isMixin", false);
        resourceProxy.addProperty("jcr:mixinTypes", new String[] {});
        return new NodeTypeResourceBuilder(resourceProxy);
    }

    private ResourceProxy resource;

    private NodeTypeResourceBuilder(ResourceProxy resource) {

        this.resource = resource;
    }

    public NodeTypeResourceBuilder setSupertypes(String[] supertypes) {

        this.resource.addProperty("jcr:supertypes", supertypes);

        return this;
    }

    public NodeTypeResourceBuilder setIsMixin(boolean isMixin) {
        this.resource.addProperty("jcr:isMixin", true);

        return this;
    }

    public ResourceProxy build() {

        return resource;
    }
}