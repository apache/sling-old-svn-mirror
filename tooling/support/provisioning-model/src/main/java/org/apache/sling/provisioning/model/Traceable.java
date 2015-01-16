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

/**
 * A traceable has an optional location.
 */
public abstract class Traceable {

    /** The location. */
    private String location;

    /**
     * Get the location.
     * The location might be the location of the model file or any other
     * means identifying where the object is defined.
     * @return The location or {@code null}.
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * Set the location.
     * @param value The new location.
     */
    public void setLocation(final String value) {
        this.location = value;
    }

    @Override
    public String toString() {
        return "Traceable [location=" + location + "]";
    }
}

