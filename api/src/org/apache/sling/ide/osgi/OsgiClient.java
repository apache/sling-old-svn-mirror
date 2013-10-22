/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.osgi;

import java.io.InputStream;

import org.osgi.framework.Version;

/**
 * The <tt>OsgiClient</tt> exposes information and actions related to the OSGi subsystem of Sling
 * 
 */
public interface OsgiClient {

    Version getBundleVersion(String bundleSymbolicName) throws OsgiClientException;

    void installBundle(InputStream in, String fileName) throws OsgiClientException;

    /**
     * Installs a bundle from a local directory
     * 
     * <p>
     * The Sling launchpad instance must have filesystem access to the specified <tt>explodedBundleLocation</tt>
     * </p>
     * 
     * @param explodedBundleLocation
     * @throws OsgiClientException
     */
    void installLocalBundle(String explodedBundleLocation) throws OsgiClientException;

    /**
     * Installs a local bundle from an already-built jar file
     * 
     * @param jarredBundle the contents of the jarred bundle
     * @param sourceLocation the source location, for informative purposes only
     * 
     * @throws OsgiClientException
     */
    void installLocalBundle(InputStream jarredBundle, String sourceLocation) throws OsgiClientException;

}