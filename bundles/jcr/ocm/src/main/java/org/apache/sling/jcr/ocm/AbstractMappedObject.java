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
package org.apache.sling.jcr.ocm;

/**
 * The <code>AbstractMappedObject</code> is a simple helper class which may be
 * extended to off-load handling of the path of mapped objects. It simply maps
 * a single field <em>path</em> to have the path of the mapped object.
 *
 * @ocm.mapped discriminator="false"
 */
public abstract class AbstractMappedObject {

    /**
     * The path of the mapped object. This is not an actual JCR node property
     * mapping but a special field taking the path of the mapped node.
     * @ocm.field path="true"
     */
    private String path;

    protected AbstractMappedObject() {
    }

    protected AbstractMappedObject(String path) {
        this.path = path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

}
