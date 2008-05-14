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
package org.apache.sling.core.impl.output;

import java.io.IOException;

/**
 * The <code>Buffer</code> interface defines an API, which will be
 * implemented by buffering output channel implementations to facilitate
 * stack management regardless of whether the channel is stream or writer
 * based.
 */
public interface Buffer {

    /**
     * Sets the new size of the buffer.
     *
     * @param buffersize The new size of the buffer. The interpretation of
     *      negative or zero values is up to the implementation.
     *
     * @throws IllegalStateException may be thrown if the implementation may
     *      not currently change the size of the buffer.
     */
    void setBufferSize(int buffersize);

    /**
     * Returns the current size of the buffer. If the implementation is
     * not currently buffering or does not support buffering at a negative
     * number must be returned.
     *
     * @return The current size of the buffer or a negative number if
     *      buffering is disabled or not supported by the implementation.
     */
    int getBufferSize();

    /**
     * Flushes the current contents of the buffer to the output destination
     * without forcing the destination to flush its contents.
     *
     * @throws IOException May be thrown if an error occurrs flushing the
     *      contents of the buffer.
     */
    void flushBuffer() throws IOException;

    /**
     * Removes the contents of the buffer.
     *
     * @throws IllegalStateException may be thrown if the implementation
     *      is not willing to clear the buffer.
     */
    void resetBuffer();
}
