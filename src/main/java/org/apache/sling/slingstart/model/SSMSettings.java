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
package org.apache.sling.slingstart.model;


/**
 * The settings of a feature.
 */
public class SSMSettings {

    public String properties;

    /**
     * validates the object and throws an IllegalStateException
     * This object needs:
     * - properties (non empty)
     *
     * @throws IllegalStateException
     */
    public void validate() {
        // check/correct values
        if ( properties == null || properties.isEmpty() ) {
            throw new IllegalStateException("settings");
        }
    }

    public void merge(final SSMSettings other) {
        if ( this.properties == null ) {
            this.properties = other.properties;
        } else {
            this.properties = this.properties + "\n" + other.properties;
        }
    }

    @Override
    public String toString() {
        return "SSMSettings [properties=" + properties + "]";
    }
}
