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
package org.apache.sling.models.testmodels.classes;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.Required;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ResourcePath;

@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class ResourcePathPartialModel {
    
    @ResourcePath(name="propertyWithSeveralPaths", injectionStrategy=InjectionStrategy.REQUIRED)
    private List<Resource> requiredResources;
    
    @ResourcePath(name="propertyWithSeveralPaths")
    @Required
    private List<Resource> requiredResources2;
    
    
    @ResourcePath(name="propertyWithMissingPaths", injectionStrategy=InjectionStrategy.OPTIONAL)
    private List<Resource> optionalResources;
    
    @Optional
    @ResourcePath(name="propertyWithMissingPaths")
    private List<Resource> optionalResources2;
    
    @ResourcePath(name="propertyWithMissingPaths", optional=true)
    private List<Resource> optionalResources3;

    public List<Resource> getRequiredResources() {
        return requiredResources;
    }

    public List<Resource> getRequiredResources2() {
        return requiredResources2;
    }

    public List<Resource> getOptionalResources() {
        return optionalResources;
    }

    public List<Resource> getOptionalResources2() {
        return optionalResources2;
    }

    public List<Resource> getOptionalResources3() {
        return optionalResources3;
    }

}
