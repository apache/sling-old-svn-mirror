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
package org.apache.sling.commons.osgi.bundleversion;

import org.osgi.framework.Version;

/** Provides bundle version information, which can be
 *  extracted from bundle files or Bundle objects.
 */
public interface BundleVersionInfo<T> {
    /** Marker used by Maven to identify snapshots */
    String SNAPSHOT_MARKER = "SNAPSHOT";
    
    /** Name of the BND attribute that provides the bundle's last modified timestamp */
    String BND_LAST_MODIFIED = "Bnd-LastModified";
    
    /** Value for {@link #getBundleLastModified} if corresponding header
     *  is not present
     */
    long BND_LAST_MODIFIED_MISSING = -1L;
    
    /** Return the source of information: underlying File or Bundle */
    T getSource();
    
    /** True if the provided data is a valid bundle */
    boolean isBundle();
    
    /** Return the bundle symbolic name, null if not available */
    String getBundleSymbolicName();
    
    /** Return the bundle version, null if not available */
    Version getVersion();
    
    /** True if the bundle version indicates a snapshot */
    boolean isSnapshot();
    
    /** Return the bundle last modification time, based on the BND_LAST_MODIFIED 
     *  manifest header, if available. This is *not* the Bundle.getLastModified()
     *  value, which refers to actions in the OSGi framework.
     *  @return BND_LAST_MODIFIED_MISSING if header not supplied */
    long getBundleLastModified();
}
