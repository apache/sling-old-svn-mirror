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

package org.apache.sling.resourceaccesssecurity.it.impl.providers.secured;


import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.resourceaccesssecurity.it.impl.providers.SimpleModifiableResourceProvider;
import org.apache.sling.resourceaccesssecurity.it.impl.providers.SimpleResourceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(metatype = true, label = "Secured ResourceProvider")
@Service(value = ResourceProvider.class)
@Properties({
        @Property(name = ResourceProvider.ROOTS, value = "/test/secured-provider/read-update" ),
        @Property(name = ResourceProvider.USE_RESOURCE_ACCESS_SECURITY, boolValue=true, propertyPrivate=true),
        @Property(name = ResourceProvider.OWNS_ROOTS, boolValue=true, propertyPrivate=true)
})
public class SecuredReadAndUpdateResourceProvider extends SimpleModifiableResourceProvider implements ResourceProvider {

}
