/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.provisioning.model;

import java.util.HashMap;
import java.util.Map;


/**
 * An additional section in the provisioning model.
 * @since 1.4
 */
public class Section
    extends Commentable {

    /** The section name. */
    private final String name;

    /** Attributes. */
    private final Map<String, String> attributes = new HashMap<String, String>();

    /** Contents. */
    private volatile String contents;

    /**
     * Construct a new feature.
     * @param name The feature name
     */
    public Section(final String name) {
        this.name = name;
    }

    /**
     * Get the name of the section.
     * @return The name or {@code null} for an anonymous feature.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get all attributes
     * @return The map of attributes.
     */
    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    /**
     * Get the contents of the section.
     * @return The contents or {@code null}.
     */
    public String getContents() {
        return contents;
    }

    /**
     * Set the contents of the section.
     * @param contents The new contents.
     */
    public void setContents(final String contents) {
        this.contents = contents;
    }

    @Override
    public String toString() {
        return "Section [name=" + name
                + ( attributes.isEmpty() ? "": ", attributes=" + attributes )
                + ( contents == null ? "" : ", contents=" + contents)
                + ( this.getLocation() != null ? ", location=" + this.getLocation() : "")
                + "]";
    }
}
