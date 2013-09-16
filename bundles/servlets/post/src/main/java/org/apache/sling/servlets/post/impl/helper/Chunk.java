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

/**
 * <code>Chunk</code> enscapsulates all chunk upload attributes.
 * 
 * @since 2.3.4
 */
public class Chunk {

    private long offset;

    private long length;

    private boolean completed;

    /**
     * Return offset of the chunk.
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Set offset value.
     */
    public void setOffsetValue(long offset) {
        this.offset = offset;
    }

    /**
     * Return length of the file parameter.
     */
    public long getLength() {
        return length;
    }

    /**
     * Set length of file parameter.
     */
    public void setLength(long length) {
        this.length = length;
    }

    /**
     * Return true if request contains last chunk as a result upload should be
     * finished. It is useful in scenarios where file streaming where file size
     * is not known in advance.
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Set complete flag
     */
    public void setCompleted(boolean complete) {
        this.completed = complete;
    }

}
