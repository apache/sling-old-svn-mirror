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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.Path;
import org.apache.sling.models.annotations.injectorspecific.ResourcePath;

@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class ResourcePathAllOptionalModel {

    @Inject
    @Path("/some/invalidpath")
    @Optional
    private Resource fromPath;

    @Inject
    @Named("propertyContainingAPath")
    @Optional
    private Resource derefProperty;
    
    @Inject
    @Path(paths={"/some/invalidpath", "/some/invalidpath2"})
    @Optional
    private Resource manyFromPathNonList;

    @ResourcePath(path = "/some/invalidpath2", optional=true)
    private Resource fromPath2;

    @ResourcePath(name = "anotherPropertyContainingAPath", optional=true)
    private Resource derefProperty2;

    public Resource getFromPath() {
        return fromPath;
    }

    public Resource getByDerefProperty() {
        return derefProperty;
    }

    public Resource getFromPath2() {
        return fromPath2;
    }

    public Resource getByDerefProperty2() {
        return derefProperty2;
    }

}
