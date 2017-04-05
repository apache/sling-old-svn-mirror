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

package org.apache.sling.servlets.post.impl.helper;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * An input stream that reads from a list of resources that can be adapted into input streams.
 */
public class ResourceIteratorInputStream extends InputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceIteratorInputStream.class);
    private int n;
    private InputStream currentStream;
    private final Iterator<Resource> iterator;
    private int streamNo = 0;

    public ResourceIteratorInputStream(Iterator<Resource> iterator) {
        this.iterator = iterator;
        while(iterator.hasNext()) {
            currentStream = iterator.next().adaptTo(InputStream.class);
            if ( currentStream != null) {
                n = 0;
                streamNo = 1;
                return;
            }
        }
        throw new IllegalArgumentException("Resource iterator does not contain any resources that can be adapted to an input stream.");
    }

    @Override
    public int read() throws IOException {
        int i = currentStream.read();
        while ( i == -1 ) {
            if ( iterator.hasNext()) {
                LOGGER.debug("Stream {} provided {} bytes. ",streamNo, n);
                currentStream = iterator.next().adaptTo(InputStream.class);
                streamNo++;
                n = 0;
                if (currentStream != null) {
                    i = currentStream.read();
                }
            } else {
                LOGGER.debug("Last Stream {} provided {} bytes. ",streamNo, n);
                return -1;
            }
        }
        n++;
        return i;
    }
}
