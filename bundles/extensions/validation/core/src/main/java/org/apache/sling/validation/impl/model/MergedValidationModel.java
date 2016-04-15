/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;

/**
 * Generates a merged validation model out of one base {@link ValidationModel} and 1 or more models to merge.
 * The resource properties and children are basically concatenated with the exception that
 * if a resource property/child with the same name is already defined in the baseModel it is not added again.
 * 
 * That way you can overwrite and even remove validation rules from the model to merge.
 *
 */
public class MergedValidationModel implements ValidationModel {

    private final ValidationModel baseModel;
    private final Map<String, ResourceProperty> resourcePropertiesMap;
    private final Map<String, ChildResource> childResourceMap;
    
    public MergedValidationModel(ValidationModel baseModel, ValidationModel... modelsToMerge) {
        this.baseModel = baseModel;
        
        // merge resource properties and child resources: all the ones from the base + different ones from the model to merge
        resourcePropertiesMap = new HashMap<String, ResourceProperty>();
        for (ResourceProperty resourceProperty : baseModel.getResourceProperties()) {
            resourcePropertiesMap.put(resourceProperty.getName(), resourceProperty);
        }
        childResourceMap = new HashMap<String, ChildResource>();
        for (ChildResource childResource : baseModel.getChildren()) {
            childResourceMap.put(childResource.getName(), childResource);
        }
        
        for (ValidationModel modelToMerge : modelsToMerge) {
            for (ResourceProperty resourceProperty : modelToMerge.getResourceProperties()) {
                // only if name is not already used, the resource property should be considered
                if (!resourcePropertiesMap.containsKey(resourceProperty.getName())) {
                    resourcePropertiesMap.put(resourceProperty.getName(), resourceProperty);
                }
            }
            for (ChildResource childResource : modelToMerge.getChildren()) {
                // only if name is not already used, the child resource should be considered
                if (!childResourceMap.containsKey(childResource.getName())) {
                    childResourceMap.put(childResource.getName(), childResource);
                }
            }
            // throw exception if the applicable path is restricted in the modelToMerge in comparison to baseModel
            for (String path : modelToMerge.getApplicablePaths()) {
                if (isPathRestricted(path, baseModel.getApplicablePaths())) {
                    String msg = String.format("The path '%s' from one of the models to merge is more specific than any of the base paths (%s)", path, Arrays.toString(baseModel.getApplicablePaths()));
                    throw new IllegalArgumentException(msg);
                }
            }
        }
    }
    
    /**
     * 
     * @param path
     * @param pathsToCompareWith
     * @return {@code true} in case the given path is either more specific or not at all overlapping with one of the pathsToCompareWith
     */
    private boolean isPathRestricted(String path, String[] pathsToCompareWith) {
        for (String basePath : pathsToCompareWith) {
            if (basePath.startsWith(path)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    @Nonnull
    public Collection<ResourceProperty> getResourceProperties() {
        return resourcePropertiesMap.values();
    }

    @Override
    @Nonnull
    public String getValidatedResourceType() {
        return baseModel.getValidatedResourceType();
    }

    @Override
    @Nonnull
    public String[] getApplicablePaths() {
        return baseModel.getApplicablePaths();
    }

    @Override
    @Nonnull
    public Collection<ChildResource> getChildren() {
        return childResourceMap.values();
    }

}
