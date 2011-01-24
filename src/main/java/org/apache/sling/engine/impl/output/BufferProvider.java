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
package org.apache.sling.engine.impl.output;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;

/**
 * The <code>BufferProvider</code> provides buffered versions of the servlet
 * output stream and the print writer.
 */
public interface BufferProvider {

    /**
     * Sets the size of the buffer to be used (by default) for output streams
     * returned by {@link #getOutputStream()} or writers returned by
     * {@link #getWriter()}
     *
     * @param size The default buffersize. Negative values or zero disable
     *            buffering.
     */
    void setBufferSize(int size);

    /**
     * @return the currently set buffer size. This is either some default buffer
     *         size or the size last set by the {@link #setBufferSize(int)}
     *         method.
     */
    int getBufferSize();

    /**
     * @return a buffered <code>ServletOutputStream</code> whose initial buffer
     *         size is set to {@link #getBufferSize()}.
     * @throws IOException If an error occurrs setting up the output stream
     */
    ServletOutputStream getOutputStream() throws IOException;

    /**
     * @return a buffered <code>PrintWriter</code> whose initial buffer size is
     *         set to {@link #getBufferSize()}.
     * @throws IOException If an error occurrs setting up the writer
     */
    PrintWriter getWriter() throws IOException;

}
