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
package org.apache.sling.launchpad.base.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.sling.launchpad.api.LaunchpadContentProvider;

/**
 * The <code>ContextConnection</code> extends the
 * <code>java.net.URLConnection</code> to provide access to a resource which
 * is available from {@link LaunchpadContentProvider} provided to {@link Sling}.
 * <p>
 * This class is implemented by actually connecting to a resource URL which is
 * provided by the resource provider and delegating the relevant method calls.
 * Currently only {@link #getContentLength()}, {@link #getContentType()},
 * {@link #getInputStream()} and {@link #getLastModified()} are supported.
 */
public class ContextConnection extends URLConnection {

    /**
     * The {@link LaunchpadContentProvider} to which requests for content access are
     * delegated.
     */
    private final LaunchpadContentProvider resourceProvider;

    /**
     * The delegatee <code>URLConnection</code> to which some of the method
     * calls are forwarded.
     */
    private URLConnection delegatee;

    /**
     * Creates an instance of this context connection.
     *
     * @param url The original URL whose path part is used to address the
     *            resource from the resource provider.
     * @param resourceProvider The {@link LaunchpadContentProvider} to which requests
     *            for content access are delegated.
     */
    ContextConnection(URL url, LaunchpadContentProvider resourceProvider) {
        super(url);
        this.resourceProvider = resourceProvider;
    }

    /**
     * Accesses the the resource from the underlaying resource provider at the
     * URL's path.
     */
    public void connect() throws IOException {
        if (!this.connected) {
            URL contextURL = resourceProvider.getResource(url.getPath());
            if (contextURL == null) {
                throw new IOException("Resource " + url.getPath()
                    + " does not exist");
            }

            delegatee = contextURL.openConnection();
            connected = true;
        }
    }

    /**
     * Returns the length in bytes of the resource or -1 if this connection has
     * not been connected yet.
     */
    public int getContentLength() {
        return (delegatee == null) ? -1 : delegatee.getContentLength();
    }

    /**
     * Returns a guess at the content type of the resource or <code>null</code>
     * if this connection has not been connected yet.
     */
    public String getContentType() {
        return (delegatee == null) ? null : delegatee.getContentType();
    }

    /**
     * Returns a <code>InputStream</code> on the resource. If this connection
     * is not connected yet, the conneciton is opened.
     *
     * @throws IOException may be thrown if an error occurrs opening the
     *             connection or accessing the content as an
     *             <code>InputStream</code>.
     */
    public InputStream getInputStream() throws IOException {
        connect();
        return delegatee.getInputStream();
    }

    /**
     * Returns the last modification timestamp of the resource or -1 if this
     * connection has not been connected yet.
     */
    public long getLastModified() {
        return (delegatee == null) ? 0 : delegatee.getLastModified();
    }
}
