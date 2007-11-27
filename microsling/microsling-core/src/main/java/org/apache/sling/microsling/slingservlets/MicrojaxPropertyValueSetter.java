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

package org.apache.sling.microsling.slingservlets;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.sling.api.request.RequestParameter;

/**
 * Sets a Property on the given Node, in some cases with a specific type and
 * value. For example, "lastModified" with an empty value is stored as the
 * current Date.
 */
class MicrojaxPropertyValueSetter {
    public static final String CREATED_FIELD = "created";
    public static final String CREATED_BY_FIELD = "createdBy";
    public static final String LAST_MODIFIED_FIELD = "lastModified";
    public static final String LAST_MODIFIED_BY_FIELD = "lastModifiedBy";

    /**
     * Set property on given node, with some automatic values when user provides
     * the field name but no value.
     * 
     * html example for testing: <input type="hidden" name="dateCreated"/>
     * <input type="hidden" name="lastModified"/> <input type="hidden"
     * name="createdBy"/> <input type="hidden" name="lastModifiedBy"/>
     */
    void setProperty(Node parent, String name, RequestParameter[] values, String typehint, boolean nodeWasJustCreated)
            throws RepositoryException {

        // set the same timestamp for all values, to ease testing
        final Calendar now = Calendar.getInstance();
        
        if (valueProvided(values)) {
            // if user provided a value, don't mess with it
            setPropertyAsIs(parent, name, values, typehint);

        } else if (CREATED_FIELD.equals(name)) {
            if (nodeWasJustCreated) {
                setCurrentDate(parent, name, now);
            }

        } else if (CREATED_BY_FIELD.equals(name)) {
            if (nodeWasJustCreated) {
                setCurrentUser(parent, name);
            }

        } else if (LAST_MODIFIED_FIELD.equals(name)) {
            setCurrentDate(parent, name, now);

        } else if (LAST_MODIFIED_BY_FIELD.equals(name)) {
            setCurrentUser(parent, name);

        } else {
            // no magic field, set value as provided
            setPropertyAsIs(parent, name, values, typehint);
        }
    }

    /** set property to the current Date */
    private void setCurrentDate(Node parent, String name, Calendar now) throws RepositoryException {
        removePropertyIfExists(parent, name);
        parent.setProperty(name, now);
    }

    /** set property to the current User id */
    private void setCurrentUser(Node parent, String name) throws RepositoryException {
        removePropertyIfExists(parent, name);
        parent.setProperty(name, parent.getSession().getUserID());
    }

    private void removePropertyIfExists(Node parent, String name) throws RepositoryException {
        if (parent.hasProperty(name)) {
            Property prop = parent.getProperty(name);
            prop.remove();
        }
    }

    /** set property without processing, except for type hints */
    private void setPropertyAsIs(Node parent, String name, RequestParameter[] values, String typehint) throws RepositoryException {
        removePropertyIfExists(parent, name);

        // no explicit typehint
        if (typehint == null) {
            // guess type based on mvp information from property
            // TODO: use old property definition to guess aswell
            if (values.length > 1) {
                final String [] stringValues = new String[values.length];
                int i = 0;
                for(RequestParameter p : values) {
                    stringValues[i++] = p.getString();
                }
                parent.setProperty(name, stringValues);
            } else {
                parent.setProperty(name, values[0].getString());
            }
        }

        // explicit typehint Date
        if ("Date".equals(typehint)) {
            parent.setProperty(name, values[0].getString(), PropertyType.DATE);
        }

        // TODO: accept more typehints including mvp
        // TODO: binary support
    }

    /** true if values contains at least one non-empty value */
    private boolean valueProvided(RequestParameter[] values) {
        boolean result = false;

        for (RequestParameter p : values) {
            final String val = p.getString();
            if(val!=null && val.length() > 0) {
                result = true;
                break;
            }
        }

        return result;
    }

}
