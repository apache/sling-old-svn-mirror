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
package org.apache.sling.installer.api.tasks;

/**
 * The state of a resource.
 * 
 * The state represents the OSGi installer's view. It might not
 * reflect the current state of the system. For example if a
 * bundle is installed through the OSGi installer, it gets the
 * state "INSTALLED". However if the admin now deinstalls the
 * bundle through any other way like e.g. the web console,
 * the artifact still has the state "INSTALLED".
 * 
 * The state "INSTALLED" might either mean installed or
 * if the attribute {@link TaskResource#ATTR_INSTALL_EXCLUDED}
 * is set on the resource, the resource is excluded from
 * installation. It gets also marked as "INSTALLED" in order
 * to mark it as "processed" for the OSGi installer.
 */
public enum ResourceState {

    INSTALL,        // the resource should be installed
    UNINSTALL,      // the resource should be uninstalled
    INSTALLED,      // the resource is installed
    UNINSTALLED,    // the resource is uninstalled
    IGNORED         // the resource has been ignored
}
