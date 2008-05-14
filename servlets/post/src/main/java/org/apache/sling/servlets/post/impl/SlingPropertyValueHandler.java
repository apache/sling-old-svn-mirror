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

package org.apache.sling.servlets.post.impl;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.value.ValueFactoryImpl;

/**
 * Sets a Property on the given Node, in some cases with a specific type and
 * value. For example, "lastModified" with an empty value is stored as the
 * current Date.
 */
class SlingPropertyValueHandler {

    /**
     * Defins a map of auto properties
     */
    private static final Map<String, AutoType> AUTO_PROPS = new HashMap<String, AutoType>();
    static {
        AUTO_PROPS.put("created", AutoType.CREATED);
        AUTO_PROPS.put("createdBy", AutoType.CREATED_BY);
        AUTO_PROPS.put("jcr:created", AutoType.CREATED);
        AUTO_PROPS.put("jcr:createdBy", AutoType.CREATED_BY);
        AUTO_PROPS.put("lastModified", AutoType.MODIFIED);
        AUTO_PROPS.put("lastModifiedBy", AutoType.MODIFIED_BY);
        AUTO_PROPS.put("jcr:lastModified", AutoType.MODIFIED);
        AUTO_PROPS.put("jcr:lastModifiedBy", AutoType.MODIFIED_BY);
    }

    /**
     * the post processor
     */
    private final SlingPostProcessor ctx;

    /**
     * current date for all properties in this request
     */
    private final Calendar now = Calendar.getInstance();

    /**
     * Constructs a propert value handler
     * @param ctx the post processor
     */
    public SlingPropertyValueHandler(SlingPostProcessor ctx) {
        this.ctx = ctx;
    }


    /**
     * Set property on given node, with some automatic values when user provides
     * the field name but no value.
     *
     * html example for testing:
     * <xmp>
     *   <input type="hidden" name="created"/>
     *   <input type="hidden" name="lastModified"/>
     *   <input type="hidden" name="createdBy" />
     *   <input type="hidden" name="lastModifiedBy"/>
     * </xmp>
     *
     * @param parent the parent node
     * @param prop the request property
     * @throws RepositoryException if a repository error occurs
     */
    void setProperty(Node parent, RequestProperty prop)
            throws RepositoryException {

        final String name = prop.getName();
        if (prop.providesValue()) {
            // if user provided a value, don't mess with it
            setPropertyAsIs(parent, prop);

        } else if (AUTO_PROPS.containsKey(name)) {
            // avoid collision with protected properties
            switch (AUTO_PROPS.get(name)) {
                case CREATED:
                    if (parent.isNew()) {
                        setCurrentDate(parent, name);
                    }
                    break;
                case CREATED_BY:
                    if (parent.isNew()) {
                        setCurrentUser(parent, name);
                    }
                    break;
                case MODIFIED:
                    setCurrentDate(parent, name);
                    break;
                case MODIFIED_BY:
                    setCurrentUser(parent, name);
                    break;
            }
        } else {
            // no magic field, set value as provided
            setPropertyAsIs(parent, prop);
        }
    }

    /**
     * Sets the property to the given date
     * @param parent parent node
     * @param name name of the property
     * @throws RepositoryException if a repository error occurs
     */
    private void setCurrentDate(Node parent, String name)
            throws RepositoryException {
        removePropertyIfExists(parent, name);
        ctx.getHtmlResponse().onModified(
            parent.setProperty(name, now).getPath()
        );
    }

    /**
     * set property to the current User id
     * @param parent parent node
     * @param name name of the property
     * @throws RepositoryException if a repository error occurs
     */
    private void setCurrentUser(Node parent, String name)
            throws RepositoryException {
        removePropertyIfExists(parent, name);
        ctx.getHtmlResponse().onModified(
            parent.setProperty(name, parent.getSession().getUserID()).getPath()
        );
    }

    /**
     * Removes the property with the given name from the parent node if it
     * exists and if it's not a mandatory property.
     *
     * @param parent the parent node
     * @param name the name of the property to remove
     * @return path of the property that was removed or <code>null</code> if
     *         it was not removed
     * @throws RepositoryException if a repository error occurs.
     */
    private String removePropertyIfExists(Node parent, String name)
            throws RepositoryException {
        if (parent.hasProperty(name)) {
            Property prop = parent.getProperty(name);
            if (!prop.getDefinition().isMandatory()) {
                String path = prop.getPath();
                prop.remove();
                return path;
            }
        }
        return null;
    }

    /**
     * set property without processing, except for type hints
     *
     * @param parent the parent node
     * @param prop the request property
     * @throws RepositoryException if a repository error occurs.
     */
    private void setPropertyAsIs(Node parent, RequestProperty prop)
            throws RepositoryException {

        // no explicit typehint
        int type = PropertyType.UNDEFINED;
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
            ctx.getHtmlResponse().onDeleted(
                removePropertyIfExists(parent, prop.getName())
            );
        } else if (values.length == 0) {
            // do not create new prop here, but clear existing
            if (parent.hasProperty(prop.getName())) {
                ctx.getHtmlResponse().onModified(
                    parent.setProperty(prop.getName(), "").getPath()
                );
            }
        } else if (values.length == 1) {
            final String removePath = removePropertyIfExists(parent, prop.getName());
            // if the provided value is the empty string, we don't have to do anything.
            if ( values[0].length() == 0 ) {
                if ( removePath != null ) {
                    ctx.getHtmlResponse().onDeleted(removePath);
                }
            } else {
                // modify property
                if (type == PropertyType.DATE) {
                    // try conversion
                    Calendar c = ctx.getDateParser().parse(values[0]);
                    if (c != null) {
                        ctx.getHtmlResponse().onModified(
                            parent.setProperty(prop.getName(), c).getPath()
                        );
                        return;
                    }
                    // fall back to default behaviour
                }
                final Property p;
                if ( type == PropertyType.UNDEFINED ) {
                    p = parent.setProperty(prop.getName(), values[0]);
                } else {
                    p = parent.setProperty(prop.getName(), values[0], type);
                }
                ctx.getHtmlResponse().onModified(p.getPath());
            }
        } else {
            removePropertyIfExists(parent, prop.getName());
            if (type == PropertyType.DATE) {
                // try conversion
                Value[] c = ctx.getDateParser().parse(values, ValueFactoryImpl.getInstance());
                if (c != null) {
                    ctx.getHtmlResponse().onModified(
                        parent.setProperty(prop.getName(), c).getPath()
                    );
                    return;
                }
                // fall back to default behaviour
            }
            final Property p;
            if ( type == PropertyType.UNDEFINED ) {
                p = parent.setProperty(prop.getName(), values);
            } else {
                p = parent.setProperty(prop.getName(), values, type);
            }
            ctx.getHtmlResponse().onModified(p.getPath());
        }
    }

    /**
     * Defines an auto property behavior
     */
    private enum AutoType {
        CREATED,
        CREATED_BY,
        MODIFIED,
        MODIFIED_BY
    }
}
