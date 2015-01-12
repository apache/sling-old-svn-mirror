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
package org.apache.sling.scripting.core.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

/**
 * Extends the ServletOutputStream to capture the results into a byte array.
 */
public final class BufferedServletOutputStream extends ServletOutputStream {
    private final ByteArrayOutputStream baops = new ByteArrayOutputStream();
    private final String encoding;

    /**
     * Constructs a new BufferedServletOutputStream.
     *
     * @param encoding the encoding string
     */
    public BufferedServletOutputStream(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Gets the byte buffer as a string.
     *
     * @return the byte buffer
     * @throws java.io.IOException
     */
    public String getBuffer() throws IOException {
        return baops.toString(encoding);
    }

    /*
     * (non-Javadoc)
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException {
        baops.reset();
        super.close();
    }

    /*
     * (non-Javadoc)
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int b) throws IOException {
        baops.write(b);
    }
}
