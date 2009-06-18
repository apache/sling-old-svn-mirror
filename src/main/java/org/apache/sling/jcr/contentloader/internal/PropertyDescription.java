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
package org.apache.sling.jcr.contentloader.internal;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;

class PropertyDescription {
    private String name;
    private String value;
    private List<String> values;
    private String type = PropertyType.TYPENAME_STRING; // default type to string

    /**
     * @return the name
     */
    String getName() {
        return this.name;
    }

    /**
     * @param name the name to set
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * @return the type
     */
    String getType() {
        return this.type;
    }

    /**
     * @param type the type to set
     */
    void setType(String type) {
        this.type = type;
    }

    /**
     * @return the value
     */
    String getValue() {
        return this.value;
    }

    /**
     * @param value the value to set
     */
    void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the values
     */
    List<String> getValues() {
        return this.values;
    }

    /**
     * @param values the values to set
     */
    void addValue(Object value) {
        if (this.values == null) {
            this.values = new ArrayList<String>();
        }

        if (value != null) {
            this.values.add(value.toString());
        }
    }

    boolean isMultiValue() {
        return this.values != null;
    }

    public int hashCode() {
        return this.getName().hashCode() * 17 + this.getType().hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof PropertyDescription)) {
            return false;
        }

        PropertyDescription other = (PropertyDescription) obj;
        return this.getName().equals(other.getName())
            && this.getType().equals(other.getType())
            && this.equals(this.getValues(), other.getValues())
            && this.equals(this.getValue(), other.getValue());
    }

    private boolean equals(Object o1, Object o2) {
        return (o1 == null) ? o2 == null : o1.equals(o2);
    }
}