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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resourcemerger.impl.StubResource;
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
    description="Specifies if the resources are read-only or can be modified."),
    @Property(name=MergedResourcePicker.TRAVERSE_PARENT, boolValue=true, propertyPrivate=true)
})
public class OverridingResourcePicker implements MergedResourcePicker {

    public static final String DEFAULT_ROOT = "/mnt/override";

    public List<Resource> pickResources(ResourceResolver resolver, String relativePath, Resource relatedResource) {
        // TODO this method can be optimised by leveraging relatedResource (similar to MergingResourcePicker)

        String absPath = "/" + relativePath;
        final List<Resource> resources = new ArrayList<Resource>();
        final Set<String> roots = new HashSet<String>();

        Resource currentTarget = resolver.getResource(absPath);

        if (currentTarget == null) {
            currentTarget = new StubResource(resolver, absPath);
        }

        resources.add(currentTarget);

        while (currentTarget != null) {
            final InheritanceRootInfo info = new InheritanceRootInfo();
            findInheritanceRoot(currentTarget, info);
            if (info.resource == null) {
                currentTarget = null;
            } else {
                final Resource inheritanceRootResource = info.resource;
                final String pathRelativeToInheritanceRoot = info.getPathRelativeToInheritanceRoot();
                final String superType = inheritanceRootResource.getResourceSuperType();

                if (superType == null
                       || roots.contains(inheritanceRootResource.getPath())) { // avoid inheritance loops
                    currentTarget = null;
                } else {
                    final String superTypeChildPath = superType + pathRelativeToInheritanceRoot;
                    final Resource superTypeResource = resolver.getResource(superTypeChildPath);
                    if (superTypeResource != null) {
                        currentTarget = superTypeResource;
                    } else {
                        currentTarget = new StubResource(resolver, superTypeChildPath);
                    }
                    resources.add(currentTarget);
                    roots.add(inheritanceRootResource.getPath());
                }
            }
        }

        Collections.reverse(resources);

        return resources;
    }

    @Override
    public List<Resource> pickResources(ResourceResolver resolver, String relativePath) {
        return pickResources(resolver, relativePath, null);
    }

    private void findInheritanceRoot(final Resource target, final InheritanceRootInfo info) {
        String superType = target.getResourceSuperType();
        if (superType != null) {
            info.resource = target;
        } else {
            Resource parent = target.getParent();
            if (parent != null) {
                info.addLevel(target.getName());
                findInheritanceRoot(parent, info);
            }
        }
    }

    // Using a value object here as a sort-of tuple because the original
    // way of calculating the relative path of the current resource from the
    // inheritance root did not deal with missing resources.
    private class InheritanceRootInfo {
        private Resource resource;
        private final StringBuilder pathRelativeToInheritanceRoot = new StringBuilder();

        private String getPathRelativeToInheritanceRoot() {
            return pathRelativeToInheritanceRoot.toString();
        }

        private void addLevel(String name) {
            pathRelativeToInheritanceRoot.insert(0, name).insert(0, '/');
        }
    }

}
