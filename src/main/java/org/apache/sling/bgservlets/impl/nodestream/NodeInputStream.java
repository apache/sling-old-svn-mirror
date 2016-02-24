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
package org.apache.sling.bgservlets.impl.nodestream;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reads data stored by a {@link NodeOutputStream}
 *  and rebuilds a continuous stream out of the
 *  multiple Properties that it creates.
 */
public class NodeInputStream extends InputStream {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** The Node under which we read our data */
    private final Node node;

    /** Computes path for stream storage */
    private final NodeStreamPath streamPath;

    /** Current stream that we are reading */
    private InputStream currentStream;

    public NodeInputStream(Node n) throws IOException {
        node = n;
        streamPath = new NodeStreamPath();
        selectNextStream();
    }

    /** Select next property to read from and open its stream */
    private void selectNextStream() throws IOException {
        streamPath.selectNextPath();
        final String propertyPath = streamPath.getNodePath() + "/" + NodeStreamPath.PROPERTY_NAME;
        try {
            if(node.hasProperty(propertyPath)) {
                final Property p = node.getProperty(propertyPath);
                currentStream = p.getStream();
                log.debug("Switched to the InputStream of Property {}", p.getPath());
            } else {
                currentStream = null;
                log.debug("Property {} not found, end of stream", node.getPath() + "/" + propertyPath);
            }
        } catch(RepositoryException re) {
            throw (IOException)new IOException("RepositoryException in selectNextProperty()").initCause(re);
        }
    }

    @Override
    public int available() throws IOException {
        return currentStream == null ? 0 : currentStream.available();
    }

    @Override
    public void close() throws IOException {
        if(currentStream != null) {
            currentStream.close();
        }
        super.close();
    }

    @Override
    public int read() throws IOException {
        if(currentStream == null) {
            return -1;
        }
        int result = currentStream.read();
        if(result == -1) {
            selectNextStream();
            return read();
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if(currentStream == null) {
            return 0;
        }
        int result = currentStream.read(b, off, len);
        if(result <= 0) {
            selectNextStream();
            return read(b, off, len);
        }
        return result;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
}
