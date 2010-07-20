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
    
    /** Counter used to build the name of Property from 
     *  which we currently read */
    private int counter;
    
    /** Current stream that we are reading */
    private InputStream currentStream;
    
    NodeInputStream(Node n) throws IOException {
        node = n;
        selectNextStream();
    }
    
    /** Select next property to read from and open its stream */
    private void selectNextStream() throws IOException {
        counter++;
        final String name = NodeOutputStream.STREAM_PROPERTY_NAME_PREFIX + counter;
        try {
            if(node.hasProperty(name)) {
                final Property p = node.getProperty(name); 
                currentStream = p.getStream();
                log.debug("Switched to the InputStream of Property {}", p.getPath());
            } else {
                currentStream = null;
                log.debug("Property {} not found, end of stream", node.getPath() + "/" + name);
            }
        } catch(RepositoryException re) {
            throw new IOException("RepositoryException in selectNextProperty()", re);
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
        if(result == 0) {
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
