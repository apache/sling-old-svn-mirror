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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.framework.Bundle;

/**
 * A Bundle based <code>UIRLConnection</code> which uses the bundle's last
 * modification time as the last modification time of the URL in contrast to the
 * (Apache Felix) URLConnection used for the bundle entry, which always returns
 * zero.
 */
public class BundleResourceURLConnection extends URLConnection {

    /** The bundle owning the resource underlying the URLConnection */
    private final Bundle bundle;

    private final String bundlePath;
    
    /** The original URLConnection */
    private URLConnection delegatee;

    protected BundleResourceURLConnection(Bundle bundle, String bundlePath, URL url) {
        super(url);

        this.bundle = bundle;
        this.bundlePath = bundlePath;
    }

    /**
     * Connects this URLConnection to access the data and metadata such as the
     * content length, last modification time and content type.
     */
    public synchronized void connect() throws IOException {
        if (!connected) {
            URL url = bundle.getEntry(bundlePath);
            if (url == null) {
                throw new IOException("Cannot find entry " + bundlePath
                    + " in bundle " + bundle + " for URL " + url);
            }

            delegatee = url.openConnection();

            connected = true;
        }
    }

    /** Returns the input stream of the Bundle provided URLConnection */
    public InputStream getInputStream() throws IOException {
        connect();

        return delegatee.getInputStream();
    }

    /** Returns the content length of the Bundle provided URLConnection */
    public int getContentLength() {
        try {
            connect();
        } catch (IOException ex) {
            return -1;
        }

        return delegatee.getContentLength();
    }

    /**
     * Returns the last modification time of the underlying bundle, which is the
     * last time the bundle was installed or updated
     */
    public long getLastModified() {
        try {
            connect();
        } catch (IOException ex) {
            return 0;
        }

        return bundle.getLastModified();
    }

    /** Returns the content type of the Bundle provided URLConnection */
    public String getContentType() {
        try {
            connect();
        } catch (IOException ex) {
            return null;
        }

        return delegatee.getContentType();
    }

}
