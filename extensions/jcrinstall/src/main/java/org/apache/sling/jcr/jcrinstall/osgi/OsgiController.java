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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/** jcrinstall component that installs/updates/removes 
 *  OSGi resources (bundles, deployment packages, configs)
 *  in the OSGi framework.
 */
public interface OsgiController {
    
    /** Install or update supplied resource 
     *  @param uri Unique identifier for the resource
     *  @param lastModified if the resource is installed, this is stored
     *      and can be retrieved using getLastModified().
     *  @param data resource contents
     *  @return one of the {@link InstallResultCode} result codes. 
     */
    int installOrUpdate(String uri, long lastModified, InputStream data) throws IOException, JcrInstallException;
    
    /** Uninstall the resource that was installed via given uri
     *  @param uri Unique identifier for the resource
     *  @param attributes metadata stored by the OsgiController, will be
     *      removed after calling this method
     */
    void uninstall(String uri) throws JcrInstallException;
    
    /** Return the list of uri for resources that have been installed 
     *  by this controller.
     */
    Set<String> getInstalledUris();
    
    /** Get the lastModified value for given uri, assuming the resource pointed
     *  to by that uri was installed.
     *  @return -1 if we don't have info for given uri
     */
    long getLastModified(String uri);
}
