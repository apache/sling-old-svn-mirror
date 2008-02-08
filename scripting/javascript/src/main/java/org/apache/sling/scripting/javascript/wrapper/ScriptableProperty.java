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
package org.apache.sling.scripting.javascript.wrapper;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.sling.scripting.javascript.helper.SlingWrapper;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class ScriptableProperty extends ScriptableObject implements SlingWrapper {

    public static final String CLASSNAME = "Property";
    public static final Class<?> [] WRAPPED_CLASSES = { Property.class };
    
    private Property property;

    public ScriptableProperty() {
    }

    public ScriptableProperty(Property property) {
        this.property = property;
    }

    public void jsConstructor() {
    }

    public void jsConstructor(Object res) {
        this.property = (Property) res;
    }

    @Override
    public String getClassName() {
        return CLASSNAME;
    }

    public Class<?> [] getWrappedClasses() {
        return WRAPPED_CLASSES;
    }
    
    public Object jsGet_value() {
        try {
            return property.getValue();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsGet_values() {
        try {
            return property.getValues();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsGet_string() {
        try {
            return property.getString();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsGet_stream() {
        try {
            return property.getStream();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsGet_long() {
        try {
            return property.getLong();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsGet_double() {
        try {
            return property.getDouble();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsGet_date() {
        try {
            return property.getDate();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsGet_boolean() {
        try {
            return property.getBoolean();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsGet_node() {
        try {
            return ScriptRuntime.toObject(this, property.getValue());
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsGet_length() {
        try {
            return property.getLength();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public long[] jsGet_lengths() {
        try {
            return property.getLengths();
        } catch (RepositoryException re) {
            return new long[0];
        }
    }

    public Object jsGet_definition() {
        try {
            return property.getDefinition();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public int getType() {
        try {
            return property.getType();
        } catch (RepositoryException re) {
            return PropertyType.UNDEFINED;
        }
    }

    public Object jsGet_session() {
        try {
            return property.getSession();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public String jsGet_path() {
        try {
            return property.getPath();
        } catch (RepositoryException e) {
            return property.toString();
        }
    }

    public String jsGet_name() {
        try {
            return property.getName();
        } catch (RepositoryException e) {
            return property.toString();
        }
    }

    public Object jsGet_parent() {
        try {
            return ScriptRuntime.toObject(this, property.getParent());
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public int jsGet_depth() {
        try {
            return property.getDepth();
        } catch (RepositoryException re) {
            return -1;
        }
    }

    public boolean jsGet_new() {
        return property.isNew();
    }

    public boolean jsGet_modified() {
        return property.isModified();
    }

    @Override
    public Object getDefaultValue(Class typeHint) {
        return toString();
    }

    @Override
    public String toString() {
        try {
            return property.getValue().getString();
        } catch (RepositoryException e) {
            return property.toString();
        }
    }
    
    //---------- Wrapper interface --------------------------------------------

    public Object unwrap() {
        return property;
    }
}
