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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Path;
import org.apache.sling.models.annotations.injectorspecific.ResourcePath;

@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class ResourcePathModel {

    @Inject
    @Path("/some/path")
    private Resource fromPath;

    @Inject
    @Named("propertyContainingAPath")
    private Resource derefProperty;
    
    @Inject
    @Path(paths={"/some/path", "/some/path2"})
    private List<Resource> manyFromPath;
    
    @ResourcePath(paths={"/some/path2","/some/path"})
    private List<Resource> manyFromPath2;

    @ResourcePath(path = "/some/path2")
    private Resource fromPath2;
    
    @ResourcePath(name="propertyWithSeveralPaths")
    private List<Resource> multipleResources;
    
    @ResourcePath
    private List<Resource> propertyWithSeveralPaths;

    @ResourcePath(name = "anotherPropertyContainingAPath")
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
    
    public List<Resource> getMultipleResources(){
    	return this.multipleResources;
    }
    
    public List<Resource> getManyFromPath(){
        return this.manyFromPath;
    }
    
    public List<Resource> getManyFromPath2(){
        return this.manyFromPath2;
    }

    public List<Resource> getPropertyWithSeveralPaths(){
        return this.propertyWithSeveralPaths;
    }
}
