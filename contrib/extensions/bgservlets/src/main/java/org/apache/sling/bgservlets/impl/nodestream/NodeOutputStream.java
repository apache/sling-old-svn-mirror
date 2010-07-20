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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** An OutputStream stored in properties under
 *  a JCR node. The content is persisted on 
 *  each flush() call, using sequentially-named
 *  properties so that {@link NodeInputStream} can
 *  reconstruct the stream from permanent storage.
 *  flush() is also called automatically every 
 *  BUFFER_SWITCH_SIZE bytes, to keep our memory
 *  requirements low.    
 *  
 *  Meant to be used when running background servlets:
 *  we want to save their output in a way that
 *  survives system restart.
 */
public class NodeOutputStream extends OutputStream {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Prefix for Property names used to store our streams */
    public static final String STREAM_PROPERTY_NAME_PREFIX = "_NODE_STREAM_";
    
    /** The Node under which we write our data */
    private final Node node;
    
    /** Counter used to build the name of Property to
     *  which we currently write */
    private int counter;
    
    /** Buffer to hold data before writing it to a Property */
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(BUFFER_SIZE);
    
    public static final int BUFFER_SIZE = 32768;
    public static final int BUFFER_SWITCH_SIZE = BUFFER_SIZE * 100 / 90;
    
    public NodeOutputStream(Node n) {
        node = n;
    }
    
    /** Calls flush to persist our stream, before closing */
    @Override
    public void close() throws IOException {
        flush();
        buffer.close();
    }

    /** Store the contents of our buffer to a new Property under our
     *  node, numbered sequentially.
     */
    @Override
    public void flush() throws IOException {
        counter++;
        final String name = NodeOutputStream.STREAM_PROPERTY_NAME_PREFIX + counter;
        try {
            if(!node.getSession().isLive()) {
                log.warn("Session closed, unable to flush stream");
            } else {
                node.setProperty(name, new ByteArrayInputStream(buffer.toByteArray()));
                log.debug("Saved {} bytes to Property {}", buffer.size(), node.getProperty(name).getPath());
                node.save();
                buffer.reset();
            }
        } catch(RepositoryException re) {
            throw new IOException("RepositoryException in flush()", re);
        }
    }
    
    private void flushIfNeeded() throws IOException {
        if(buffer.size() >= BUFFER_SWITCH_SIZE) {
            log.debug("Buffer size {} reached switch level {}, flushing", buffer.size(), BUFFER_SWITCH_SIZE);
            flush();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        buffer.write(b, off, len);
        flushIfNeeded();
    }

    @Override
    public void write(byte[] b) throws IOException {
        buffer.write(b);
        flushIfNeeded();
    }

    @Override
    public void write(int b) throws IOException {
        buffer.write(b);
        flushIfNeeded();
    }
}
