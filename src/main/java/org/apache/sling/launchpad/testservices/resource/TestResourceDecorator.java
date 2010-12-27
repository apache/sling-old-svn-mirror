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
package org.apache.sling.launchpad.testservices.resource;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceWrapper;

/** Test ResourceDecorator that sets a specific resource type
 *  for resources having a path that starts
 *  with /testing + simple name of this class + /
 */
@Component
@Service
public class TestResourceDecorator implements ResourceDecorator {

    private final String PATH = "/testing/" + getClass().getSimpleName() + "/";
    private final String RESOURCE_TYPE = "TEST_RESOURCE_DECORATOR_RESOURCE_TYPE";
    
    public Resource decorate(Resource resource, HttpServletRequest request) {
        return decorate(resource);
    }

    public Resource decorate(Resource resource) {
        if(resource.getPath().startsWith(PATH)) {
            return new ResourceWrapper(resource) {
                @Override
                public String getResourceType() {
                    return RESOURCE_TYPE;
                }
            };
        }
        return resource;
    }
}
