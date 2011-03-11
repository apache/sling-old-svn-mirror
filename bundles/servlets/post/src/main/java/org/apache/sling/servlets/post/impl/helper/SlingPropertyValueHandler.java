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

package org.apache.sling.servlets.post.impl.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Sets a Property on the given Node, in some cases with a specific type and
 * value. For example, "lastModified" with an empty value is stored as the
 * current Date.
 */
public class SlingPropertyValueHandler {

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
    private final List<Modification> changes;

    private final DateParser dateParser;

    private final ReferenceParser referenceParser;

    /**
     * current date for all properties in this request
     */
    private final Calendar now = Calendar.getInstance();

    // hard-coding WEAKREFERENCE as propertyType = 10 because we don'
    // want to depend upon jcr 2 api just for the constant.
    private static final int PROPERTY_TYPE_WEAKREFERENCE = 10;

    /**
     * Constructs a propert value handler
     */
    public SlingPropertyValueHandler(DateParser dateParser, ReferenceParser referenceParser, List<Modification> changes) {
        this.dateParser = dateParser;
        this.referenceParser = referenceParser;
        this.changes = changes;
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
    public void setProperty(Node parent, RequestProperty prop)
            throws RepositoryException {

        final String name = prop.getName();
        if (prop.providesValue()) {
            // if user provided a value, don't mess with it
            setPropertyAsIs(parent, prop);

        } else if (AUTO_PROPS.containsKey(name)) {
            // avoid collision with protected properties
            try {
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
            } catch (ConstraintViolationException e) {
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
        changes.add(Modification.onModified(
            parent.setProperty(name, now).getPath()
        ));
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
        changes.add(Modification.onModified(
            parent.setProperty(name, parent.getSession().getUserID()).getPath()
        ));
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

        String[] values = prop.getStringValues();

        if (values == null || (values.length == 1 && values[0].length() == 0)) {
            // if no value is present or a single empty string is given,
            // just remove the existing property (if any)
            removeProperty(parent, prop);

        } else if (values.length == 0) {
            // do not create new prop here, but clear existing
            clearProperty(parent, prop);

        } else {
            // when patching, simply update the value list using the patch operations
            if (prop.isPatch()) {
                values = patch(parent, prop.getName(), values);
                if (values == null) {
                    return;
                }
            }

            final ValueFactory valFac = parent.getSession().getValueFactory();

            final boolean multiValue = isMultiValue(parent, prop, values);
            final int type = getType(parent, prop);

            if (multiValue) {
                // converting single into multi value props requires deleting it first
                removeIfSingleValueProperty(parent, prop);
            }

            if (type == PropertyType.DATE) {
                if (storeAsDate(parent, prop.getName(), values, multiValue, valFac)) {
                    return;
                }
            } else if (isReferencePropertyType(type)) {
                if (storeAsReference(parent, prop.getName(), values, type, multiValue, valFac)) {
                    return;
                }
            }

            store(parent, prop.getName(), values, type, multiValue);
        }
    }

    /**
     * Patches a multi-value property using add and remove operations per value.
     */
    private String[] patch(Node parent, String name, String[] values) throws RepositoryException {
        // we do not use a Set here, as we want to be very restrictive in our
        // actions and avoid touching elements that are not modified through the
        // add/remove patch operations; e.g. if the value "foo" occurs twice
        // in the existing array, and is not touched, afterwards there should
        // still be two times "foo" in the list, even if this is not a real set.
        List<String> oldValues = new ArrayList<String>();

        if (parent.hasProperty(name)) {
            Property p = parent.getProperty(name);

            // can only patch multi-value props
            if (!p.getDefinition().isMultiple()) {
                return null;
            }

            for (Value v : p.getValues()) {
                oldValues.add(v.getString());
            }
        }

        boolean modified = false;
        for (String v : values) {
            if (v != null && v.length() > 0) {
                final char op = v.charAt(0);
                final String val = v.substring(1);

                if (op == SlingPostConstants.PATCH_ADD) {
                    if (!oldValues.contains(val)) {
                        oldValues.add(val);
                        modified = true;
                    }
                } else if (op == SlingPostConstants.PATCH_REMOVE) {
                    while (oldValues.remove(val)) {
                        modified = true;
                    }
                }
            }
        }

        // if the patch does not include any operations (e.g. invalid ops)
        // return null to indicate that nothing should be done
        if (modified) {
            return oldValues.toArray(new String[oldValues.size()]);
        }

        return null;
    }


    private boolean isReferencePropertyType(int propertyType) {
        return propertyType == PropertyType.REFERENCE || propertyType == PROPERTY_TYPE_WEAKREFERENCE;
    }

    private boolean isWeakReference(int propertyType) {
        return propertyType == PROPERTY_TYPE_WEAKREFERENCE;
    }

    /**
     * Returns the property type to use for the given property. This is defined
     * either by an explicit type hint in the request or simply the type of the
     * existing property.
     */
    private int getType(Node parent, RequestProperty prop) throws RepositoryException {
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
        if ( type == PropertyType.UNDEFINED && values != null && values.length > 0 ) {
            if ( parent.hasProperty(prop.getName()) ) {
                type = parent.getProperty(prop.getName()).getType();
            }
        }
        return type;
    }

    /**
     * Returns whether the property should be handled as multi-valued.
     */
    private boolean isMultiValue(Node parent, RequestProperty prop, String[] values) throws RepositoryException {
        // multiple values are provided
        if (values != null && values.length > 1) {
            return true;
        }
        // TypeHint with []
        if (prop.hasMultiValueTypeHint()) {
            return true;
        }
        // patch method requires multi value
        if (prop.isPatch()) {
            return true;
        }
        // nothing in the request, so check the current JCR property definition
        if (parent.hasProperty(prop.getName()) ) {
            return parent.getProperty(prop.getName()).getDefinition().isMultiple();
        }
        return false;
    }

    /**
     * Clears a property: sets an empty string for single-value properties, and
     * removes multi-value properties.
     */
    private void clearProperty(Node parent, RequestProperty prop) throws RepositoryException {
        if (parent.hasProperty(prop.getName())) {
            if (parent.getProperty(prop.getName()).getDefinition().isMultiple()) {
                //the existing property is multi-valued, so just delete it?
                final String removePath = removePropertyIfExists(parent, prop.getName());
                if ( removePath != null ) {
                    changes.add(Modification.onDeleted(removePath));
                }
            } else {
                changes.add(Modification.onModified(
                    parent.setProperty(prop.getName(), "").getPath()
                ));
            }
        }
    }

    /**
     * Removes the property if it exists.
     */
    private void removeProperty(Node parent, RequestProperty prop) throws RepositoryException {
        final String removePath = removePropertyIfExists(parent, prop.getName());
        if ( removePath != null ) {
            changes.add(Modification.onDeleted(removePath));
        }
    }

    /**
     * Removes the property if it exists and is single-valued.
     */
    private void removeIfSingleValueProperty(Node parent, RequestProperty prop) throws RepositoryException {
        if (parent.hasProperty(prop.getName())) {
            if (!parent.getProperty(prop.getName()).getDefinition().isMultiple()) {
                // the existing property is single-valued, so we have to delete it before setting the
                // multi-value variation
                final String removePath = removePropertyIfExists(parent, prop.getName());
                if ( removePath != null ) {
                    changes.add(Modification.onDeleted(removePath));
                }
            }
        }
    }

    /**
     * Stores property value(s) as date(s). Will parse the date(s) from the string
     * value(s) in the {@link RequestProperty}.
     *
     * @return true only if parsing was successfull and the property was actually changed
     */
    private boolean storeAsDate(Node parent, String name, String[] values, boolean multiValued, ValueFactory valFac) throws RepositoryException {
        if (multiValued) {
            Value[] array = dateParser.parse(values, valFac);
            if (array != null) {
                changes.add(Modification.onModified(
                    parent.setProperty(name, array).getPath()
                ));
                return true;
            }
        } else {
            if (values.length >= 1) {
                Calendar c = dateParser.parse(values[0]);
                if (c != null) {
                    changes.add(Modification.onModified(
                        parent.setProperty(name, c).getPath()
                    ));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Stores property value(s) as reference(s). Will parse the reference(s) from the string
     * value(s) in the {@link RequestProperty}.
     *
     * @return true only if parsing was successfull and the property was actually changed
     */
    private boolean storeAsReference(Node parent, String name, String[] values, int type, boolean multiValued, ValueFactory valFac) throws RepositoryException {
        if (multiValued) {
            Value[] array = referenceParser.parse(values, valFac, isWeakReference(type));
            if (array != null) {
                changes.add(Modification.onModified(
                    parent.setProperty(name, array).getPath()
                ));
                return true;
            }
        } else {
            if (values.length >= 1) {
                Value v = referenceParser.parse(values[0], valFac, isWeakReference(type));
                if (v != null) {
                    changes.add(Modification.onModified(
                        parent.setProperty(name, v).getPath()
                    ));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Stores the property as string or via a strign value, but with an explicit
     * type. Both multi-value or single-value.
     */
    private void store(Node parent, String name, String[] values, int type, boolean multiValued /*, ValueFactory valFac */) throws RepositoryException {
        Property p = null;

        if (multiValued) {
            if (type == PropertyType.UNDEFINED) {
                p = parent.setProperty(name, values);
            } else {
                p = parent.setProperty(name, values, type);
            }
        } else if (values.length >= 1) {
            if (type == PropertyType.UNDEFINED) {
                p = parent.setProperty(name, values[0]);
            } else {
                p = parent.setProperty(name, values[0], type);
            }
        }

        if (p != null) {
            changes.add(Modification.onModified(p.getPath()));
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
