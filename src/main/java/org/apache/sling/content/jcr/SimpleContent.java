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
package org.apache.sling.content.jcr;

import org.apache.sling.component.Content;

/**
 * The <code>SimpleContent</code> class is an abstract base class helping to
 * map any repository node. This base class is intended to be extended by all
 * classes implementing the <code>Content</code> interface whose data is
 * mapped to and from JCR repository content. There is however no requirement to
 * extend this class.
 * <p>
 * This class is defined with a content mapping for the <code>nt:base</code>
 * node type (the root node type) mapping the path of the node to the
 * {@link #getPath() path} field.
 *
 * @ocm.mapped jcrNodeType="nt:base" discriminator="false"
 */
public abstract class SimpleContent implements Content {

    /**
     * The path of the node to which this instance belongs
     *
     * @ocm.field path="true"
     */
    private String path;

    /**
     * Returns the path of the repository node providing the persistence of this
     * content object.
     * <p>
     * This method should not normally be overwritten by extending classes.
     * Doing so may render content loading and storing instable.
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Sets the path of the repository node providing the persistence of this
     * content object.
     * <p>
     * This method is not part of the public API of this class and therefore not
     * intended to be used by client code or extending classes. It is handled
     * internally by the Content Management functionality.
     *
     * @param path The path of the node from which this instance has been
     *            loaded.
     */
    public void setPath(String path) {
        this.path = path;
    }
}
