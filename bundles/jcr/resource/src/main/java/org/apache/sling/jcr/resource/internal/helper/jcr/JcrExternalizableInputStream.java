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

package org.apache.sling.jcr.resource.internal.helper.jcr;


import org.apache.sling.api.resource.ExternalizableInputStream;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * A lazy initialised input stream wrapping a JCR Property that also has a URI representation. The
 * InputStream will be initialised when the first byte is read from the input stream so that the URI
 * can be used without consuming any local IO resources.
 */
public class JcrExternalizableInputStream extends InputStream implements ExternalizableInputStream {
    private final Property data;
    private final URI uri;
    private InputStream inputStream;

    JcrExternalizableInputStream(Property data, URI uri) {
        this.data = data;
        this.uri = uri;
    }

    @Override
    public int read() throws IOException {
        return getInputStream().read();
    }

    private InputStream getInputStream() throws IOException {
        if ( inputStream == null) {
            try {
                // perform lazy initialisation so that a consumer of
                // this object can use the getURI method without triggering
                // local IO operations. A DataSource implementation that
                // converts the JCR Property to an InputStream might make a local
                // copy. This avoids that IO operation.
                inputStream = data.getBinary().getStream();
            } catch (RepositoryException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return inputStream;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public URI getPrivateURI() {
        // Private URIs are not provided by this implementation.
        return null;
    }
}
