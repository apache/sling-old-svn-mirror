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
package org.apache.sling.jcr.jcrinstall.osgi;

import java.util.Map;


/** Interface for components that can process OSGi "resources",
 *  that is bundles, deployment packages, configs, etc.
 */
public interface OsgiResourceProcessor {
    
    /** True if this processor can process the given uri */
    boolean canProcess(String uri);
    
    /** Install or update supplied resource 
     *  @param uri Unique identifier for the resource
     *  @param attributes metadata stored by the OsgiController, can be used to
     *      store additional information
     *  @param data The data to install
     *  @return one of the {@link InstallResultCode} result codes. 
     */
    int installOrUpdate(String uri, Map<String, Object> attributes, InstallableData data) throws Exception;
    
    /** Uninstall the resource that was installed via given uri
     *  @param uri Unique identifier for the resource
     *  @param attributes metadata stored by the OsgiController, will be
     *      removed after calling this method
     */
    void uninstall(String uri, Map<String, Object> attributes) throws Exception;
    
    /** Process our installer queue, if needed, for example by trying
     *  to start outstanding bundles.
     */
    void processResourceQueue() throws Exception;
    
    /**
     * Called to cleanup the resource processor when it is not needed anymore.
     */
    void dispose();
}
