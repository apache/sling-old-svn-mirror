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
package org.apache.sling.jcr.classloader.internal.net;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.apache.sling.jcr.classloader.internal.ClassLoaderWriterImpl;

/**
 * The <code>JCRURLHandler</code> is the <code>URLStreamHandler</code> for
 * JCR Repository URLs identified by the scheme <code>jcr</code>.
 * <p>
 * JCR Repository URLs have not been standardized yet and may only be created
 * in the context of an existing <code>Session</code>. Therefore this handler
 * is not globally available and JCR Repository URLs may only be created through
 * the factory method.
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 *
 * @see org.apache.sling.jcr.classloader.internal.net.JCRURLConnection
 */
public class JCRURLHandler extends URLStreamHandler {

    /**
     * The scheme for JCR Repository URLs (value is "jcr").
     */
    private static final String REPOSITORY_SCHEME = "jcr";

    /**
     * The writer provides access to a new session.
     *
     * @see #createSession()
     */
    private final ClassLoaderWriterImpl writer;

    /**
     * The repository path to the underlying item.
     */
    private final String path;

    /**
     * Creates a new JCR Repository URL for the given session and item path.
     *
     * @param writer The writer session providing access to the item.
     * @param path The absolute path to the item. This must be an absolute
     *      path with a leading slash character. If this is <code>null</code>
     *      the root node path - <code>/</code> - is assumed.
     *
     * @return The JCR Repository URL
     *
     * @throws MalformedURLException If an error occurrs creating the
     *      <code>URL</code> instance.
     */
    public static URL createURL(final ClassLoaderWriterImpl writer, final String path)
        throws MalformedURLException {
        return new URL(REPOSITORY_SCHEME, "", -1,
            path,
            new JCRURLHandler(writer, path));
    }

    /**
     * Creates a new instance of the <code>JCRURLHandler</code> with the
     * given session.
     *
     * @param writer The dynamic class loader writer
     *
     * @throws NullPointerException if <code>session</code> is <code>null</code>.
     */
    JCRURLHandler(final ClassLoaderWriterImpl writer, final String path) {
        if (writer == null) {
            throw new NullPointerException("writer");
        }

        this.writer = writer;
        this.path = path;
    }

    /**
     * Returns the session supporting this handler.
     */
    ClassLoaderWriterImpl getClassLoaderWriter() {
        return this.writer;
    }

    String getPath() {
        return this.path;
    }

    //---------- URLStreamHandler abstracts ------------------------------------

    /**
     * Gets a connection object to connect to an JCR Repository URL.
     *
     * @param url The JCR Repository URL to connect to.
     *
     * @return An instance of the {@link JCRURLConnection} class.
     *
     * @see JCRURLConnection
     */
    protected URLConnection openConnection(final URL url) {
        return new JCRURLConnection(url, this);
    }
}
