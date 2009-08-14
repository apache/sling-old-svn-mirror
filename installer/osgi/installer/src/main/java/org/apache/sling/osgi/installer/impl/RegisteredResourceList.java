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
package org.apache.sling.osgi.installer.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.sling.osgi.installer.impl.tasks.BundleInstallTask;

/** Used by the installer to hold the complete list of RegisteredResources,
 *  this class is key in defining priorities between resources, making sure
 *  the latest version of a bundle is installed if several are available, etc.
 *  
 *  The storage structure is not really a list, but a Map of sorted lists of
 *  RegisteredResource. The Map key is the "OSGi entity" that the resources
 *  represent, for example a given bundle (keyed by symbolic name) or 
 *  a given config (keyed by pid).
 *  
 *  Before it starts executing OSGi tasks, the installer calls the 
 *  getTasks() method which walks the data structure and uses priority rules
 *  and the RegisteredResource's desired and actual states to define the set
 *  of OSGi tasks to execute.
 *  
 *  This class is *not* threadsafe, clients must care about that.
 */
class RegisteredResourceList {
    private final Map<String, TreeSet<RegisteredResource>> list = new HashMap<String, TreeSet<RegisteredResource>>();
    
    /** Add the given resource to this list, at the correct position based
     *  on the resource's entity ID */
    void add(RegisteredResource r) {
        TreeSet<RegisteredResource> t = list.get(r.getEntityId());
        if(t == null) {
            t = new TreeSet<RegisteredResource>(new RegisteredResourceComparator());
            list.put(r.getEntityId(), t);
        }
        t.add(r);
    }
    
    List<OsgiInstallerTask> getTasks() {
        final List<OsgiInstallerTask> result = new LinkedList<OsgiInstallerTask>();
        
        // Walk the list of entities, and create appropriate OSGi tasks for each group
        for(TreeSet<RegisteredResource> group : list.values()) {
            for(RegisteredResource r : group) {
                // TODO tasks must be created based on priorities + desired/actual states
                if(r.getResourceType().equals(RegisteredResource.ResourceType.BUNDLE)) {
                    result.add(new BundleInstallTask(r));
                }
            }
        }
        
        return result;
    }
}
