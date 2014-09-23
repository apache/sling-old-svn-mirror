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
package org.apache.sling.resourcemerger.impl.picker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker;

@Component(name = "org.apache.sling.resourcemerger.picker.overriding",
        label = "Apache Sling Overriding Resource Picker",
    description = "This resource picker delivers merged resources based on the resource type hierarchy.",
    metatype = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = MergedResourcePicker.MERGE_ROOT, value = OverridingResourcePicker.DEFAULT_ROOT,
            label = "Root", description = "Root path at which merged resources will be available."),
    @Property(name=MergedResourcePicker.READ_ONLY, boolValue=true,
    label="Read Only",
    description="Specifies if the resources are read-only or can be modified.")

})
public class OverridingResourcePicker implements MergedResourcePicker {

    public static final String DEFAULT_ROOT = "/mnt/override";

    public List<Resource> pickResources(ResourceResolver resolver, String relativePath) {
        String absPath = "/" + relativePath;
        final List<Resource> resources = new ArrayList<Resource>();

        Resource currentTarget = resolver.getResource(absPath);

        if (currentTarget != null) {
            resources.add(currentTarget);

            while (currentTarget != null) {
                final Resource inheritanceRootResource = findInheritanceRoot(currentTarget);
                if (inheritanceRootResource == null) {
                    currentTarget = null;
                } else {
                    final String relPath = currentTarget.getPath()
                            .substring(inheritanceRootResource.getPath().length());
                    final String superType = inheritanceRootResource.getResourceSuperType();
                    if (superType == null) {
                        currentTarget = null;
                    } else {
                        final String superTypeChildPath = superType + relPath;
                        final Resource superTypeResource = resolver.getResource(superTypeChildPath);
                        if (superTypeResource != null) {
                            resources.add(superTypeResource);
                            currentTarget = superTypeResource;
                        } else {
                            resources.add(new NonExistingResource(resolver, superTypeChildPath));
                            currentTarget = null;
                        }
                    }
                }
            }

            Collections.reverse(resources);
        }
        return resources;
    }

    private Resource findInheritanceRoot(Resource target) {
        String superType = target.getResourceSuperType();
        if (superType != null) {
            return target;
        } else {
            Resource parent = target.getParent();
            if (parent == null) {
                return null;
            } else {
                return findInheritanceRoot(parent);
            }
        }
    }

}
