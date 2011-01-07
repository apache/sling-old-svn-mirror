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
package org.apache.sling.installer.api.tasks;

import java.io.InputStream;

/**
 * A result of a {@link ResourceTransformer}.
 */
public class TransformationResult {

    /** A new resource type. */
    private String resourceType;

    /** A new input stream. */
    private InputStream inputStream;

    /**
     * Get the new resource type
     * @return New resource type or <code>null</code>.
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Get the new input stream
     * @return New input stream or <code>null</code>.
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Set a new resource type.
     * @param resourceType The resource type
     */
    public void setResourceType(final String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * Set a new input stream.
     * @param inputStream The input stream
     */
    public void setInputStream(final InputStream inputStream) {
        this.inputStream = inputStream;
    }
}
