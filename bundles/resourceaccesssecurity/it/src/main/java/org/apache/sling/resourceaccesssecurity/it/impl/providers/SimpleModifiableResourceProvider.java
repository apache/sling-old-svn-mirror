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

package org.apache.sling.resourceaccesssecurity.it.impl.providers;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleModifiableResourceProvider extends SimpleResourceProvider {
    static Map<String, Object> properties = new ConcurrentHashMap<String, Object>();

    static {
        properties.put("initialProperty", "initialValue");
    }


    @Override
    public Resource getResource(ResourceResolver resourceResolver, String path) {
        Resource resource = super.getResource(resourceResolver, path);

        if (resource != null) {
            resource = new ModifiableResource(resource);
        }

        return resource;
    }


    public class ModifiableResource extends ResourceWrapper {
        /**
         * Creates a new wrapper instance delegating all method calls to the given
         * <code>resource</code>.
         */
        public ModifiableResource(Resource resource) {
            super(resource);
        }

        @Override
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if (type.equals(ModifiableValueMap.class)) {
                return (AdapterType) new ModifiableValueMapDecorator(properties);
            }
            else if (type.equals(Map.class) || type.equals(ValueMap.class)) {
                return (AdapterType) new ValueMapDecorator(properties);
            }

            return super.adaptTo(type);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }
}
