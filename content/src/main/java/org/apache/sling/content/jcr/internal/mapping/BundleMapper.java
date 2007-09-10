/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.content.jcr.internal.mapping;

import java.util.Map;

import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;

public class BundleMapper implements Mapper {

    private MappingDescriptor mappingDescriptor;

    BundleMapper(MappingDescriptor mappingDescriptor) {
        this.mappingDescriptor = mappingDescriptor;
    }
    
    public ClassDescriptor getClassDescriptorByClass(Class clazz) {
        return mappingDescriptor.getClassDescriptorByName(clazz.getName());
    }

    public ClassDescriptor getClassDescriptorByNodeType(String jcrNodeType) {
        return mappingDescriptor.getClassDescriptorByNodeType(jcrNodeType);
    }
    
    String[] getMappedClasses() {
        return toStringArray(mappingDescriptor.getClassDescriptorsByClassName());
    }
    
    String[] getMappedNodeTypes() {
        return toStringArray(mappingDescriptor.getClassDescriptorsByNodeType());
    }
    
    private String[] toStringArray(Map map) {
        return (String[]) map.keySet().toArray(new String[map.size()]);
    }
}
