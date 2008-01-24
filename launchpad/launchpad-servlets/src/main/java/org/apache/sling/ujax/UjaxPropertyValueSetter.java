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

package org.apache.sling.ujax;

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
class UjaxPropertyValueSetter {
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
    void setProperty(Node parent, RequestProperty prop, boolean nodeWasJustCreated)
            throws RepositoryException {

        // set the same timestamp for all values, to ease testing
        final Calendar now = Calendar.getInstance();

        final String name = prop.getName();
        if (prop.providesValue()) {
            // if user provided a value, don't mess with it
            setPropertyAsIs(parent, prop);

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
            setPropertyAsIs(parent, prop);
        }
    }

    /** set property to the current Date */
    private void setCurrentDate(Node parent, String name, Calendar now)
            throws RepositoryException {
        removePropertyIfExists(parent, name);
        parent.setProperty(name, now);
    }

    /** set property to the current User id */
    private void setCurrentUser(Node parent, String name)
            throws RepositoryException {
        removePropertyIfExists(parent, name);
        parent.setProperty(name, parent.getSession().getUserID());
    }

    /**
     * Removes the property with the given name from the parent node if it
     * exists and if it's not a mandatory property.
     *
     * @param parent the parent node
     * @param name the name of the property to remove
     * @throws RepositoryException if a repository error occurs.
     */
    private void removePropertyIfExists(Node parent, String name)
            throws RepositoryException {
        if (parent.hasProperty(name)) {
            Property prop = parent.getProperty(name);
            if (!prop.getDefinition().isMandatory()) {
                prop.remove();
            }
        }
    }

    /** set property without processing, except for type hints */
    private void setPropertyAsIs(Node parent, RequestProperty prop)
            throws RepositoryException {

        removePropertyIfExists(parent, prop.getName());

        // no explicit typehint
        int type = PropertyType.STRING;
        if (prop.getTypeHint() != null) {
            try {
                type = PropertyType.valueFromName(prop.getTypeHint());
            } catch (Exception e) {
                // ignore
            }
        }

        String[] values = prop.getStringValues();
        if (values == null) {
            // remove property
            removePropertyIfExists(parent, prop.getName());
        } else if (values.length == 0) {
            // do not create new prop here, but clear existing
            if (parent.hasProperty(prop.getName())) {
                parent.setProperty(prop.getName(), "");
            }
        } else if (values.length == 1) {
            removePropertyIfExists(parent, prop.getName());
            parent.setProperty(prop.getName(), values[0], type);
        } else {
            removePropertyIfExists(parent, prop.getName());
            parent.setProperty(prop.getName(), values, type);
        }
    }

}
