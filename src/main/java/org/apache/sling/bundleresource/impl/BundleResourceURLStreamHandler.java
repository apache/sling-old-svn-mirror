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
package org.apache.sling.bundleresource.impl;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.osgi.framework.Bundle;

class BundleResourceURLStreamHandler extends URLStreamHandler {

    static final String PROTOCOL = "bundle";

    private final Bundle bundle;
    
    private final String bundlePath;

    BundleResourceURLStreamHandler(Bundle bundle, String bundlePath) {
        this.bundle = bundle;
        this.bundlePath = bundlePath;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        if (!PROTOCOL.equals(u.getProtocol())) {
            throw new IOException("Cannot open connection to " + u
                + ", wrong protocol");
        }

        return new BundleResourceURLConnection(bundle, bundlePath, u);
    }

}
