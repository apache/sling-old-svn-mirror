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
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
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
    public void setProperty(final Resource parent, final RequestProperty prop)
    throws RepositoryException, PersistenceException {
        final Modifiable mod = new Modifiable();
        mod.resource = parent;
        mod.node = parent.adaptTo(Node.class);
        mod.valueMap = parent.adaptTo(ModifiableValueMap.class);
        if ( mod.valueMap == null ) {
            throw new PersistenceException("Resource at '" + parent.getPath() + "' is not modifiable.");
        }

        final String name = prop.getName();
        if (prop.providesValue()) {
            // if user provided a value, don't mess with it
            setPropertyAsIs(mod, prop);

        } else if (AUTO_PROPS.containsKey(name)) {
            // avoid collision with protected properties
            final boolean isNew = (mod.node != null ? mod.node.isNew() : true);
            try {
                switch (AUTO_PROPS.get(name)) {
                    case CREATED:
                        if (isNew) {
                            setCurrentDate(mod, name);
                        }
                        break;
                    case CREATED_BY:
                        if (isNew) {
                            setCurrentUser(mod, name);
                        }
                        break;
                    case MODIFIED:
                        setCurrentDate(mod, name);
                        break;
                    case MODIFIED_BY:
                        setCurrentUser(mod, name);
                        break;
                }
            } catch (ConstraintViolationException e) {
            }
        } else {
            // no magic field, set value as provided
            setPropertyAsIs(mod, prop);
        }
    }

    /**
     * Sets the property to the given date
     * @param parent parent node
     * @param name name of the property
     * @throws RepositoryException if a repository error occurs
     */
    private void setCurrentDate(Modifiable parent, String name)
    throws RepositoryException, PersistenceException {
        removePropertyIfExists(parent, name);
        parent.valueMap.put(name, now);
        changes.add(Modification.onModified(parent.resource.getPath() + '/' + name));
    }

    /**
     * set property to the current User id
     * @param parent parent node
     * @param name name of the property
     * @throws RepositoryException if a repository error occurs
     */
    private void setCurrentUser(Modifiable parent, String name)
    throws RepositoryException, PersistenceException {
        removePropertyIfExists(parent, name);
        final String user = parent.resource.getResourceResolver().getUserID();
        parent.valueMap.put(name, user);
        changes.add(Modification.onModified(parent.resource.getPath() + '/' + name));
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
    private String removePropertyIfExists(final Modifiable parent, final String name)
    throws RepositoryException, PersistenceException {
        if (parent.valueMap.containsKey(name) ) {
            if ( parent.node != null ) {
                final Property prop = parent.node.getProperty(name);
                if (!prop.getDefinition().isMandatory()) {
                    final String path = prop.getPath();
                    prop.remove();
                    return path;
                }
            } else {
                parent.valueMap.remove(name);
                return parent.resource.getPath() + '/' + name;
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
    private void setPropertyAsIs(final Modifiable parent, final RequestProperty prop)
    throws RepositoryException, PersistenceException {

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

            // TODO - we should check for session
            final ValueFactory valFac = parent.resource.getResourceResolver().adaptTo(Session.class).getValueFactory();

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
    private String[] patch(final Modifiable parent, String name, String[] values)
    throws RepositoryException, PersistenceException {
        // we do not use a Set here, as we want to be very restrictive in our
        // actions and avoid touching elements that are not modified through the
        // add/remove patch operations; e.g. if the value "foo" occurs twice
        // in the existing array, and is not touched, afterwards there should
        // still be two times "foo" in the list, even if this is not a real set.
        List<String> oldValues = new ArrayList<String>();

        if (parent.valueMap.containsKey(name)) {
            if ( parent.node != null ) {
                final Property p = parent.node.getProperty(name);

                // can only patch multi-value props
                if (!p.getDefinition().isMultiple()) {
                    return null;
                }

                for (Value v : p.getValues()) {
                    oldValues.add(v.getString());
                }
            } else {
                final String[] setValues = parent.valueMap.get(name, String[].class);
                if ( setValues != null ) {
                    for(final String v : setValues) {
                        oldValues.add(v);
                    }
                }
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
    private int getType(final Modifiable parent, RequestProperty prop) throws RepositoryException {
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
            if ( parent.node != null ) {
                if ( parent.node.hasProperty(prop.getName()) ) {
                    type = parent.node.getProperty(prop.getName()).getType();
                }
            }
        }
        return type;
    }

    /**
     * Returns whether the property should be handled as multi-valued.
     */
    private boolean isMultiValue(final Modifiable parent, RequestProperty prop, String[] values) throws RepositoryException {
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
        if ( parent.node != null ) {
            if (parent.node.hasProperty(prop.getName()) ) {
                return parent.node.getProperty(prop.getName()).getDefinition().isMultiple();
            }
        } else {
            final Object value = parent.valueMap.get(prop.getName());
            if ( value != null && value.getClass().isArray() ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears a property: sets an empty string for single-value properties, and
     * removes multi-value properties.
     */
    private void clearProperty(final Modifiable parent, RequestProperty prop)
    throws RepositoryException, PersistenceException {
        if (parent.valueMap.containsKey(prop.getName())) {
            if ( parent.node != null ) {
                if (parent.node.getProperty(prop.getName()).getDefinition().isMultiple()) {
                    //the existing property is multi-valued, so just delete it?
                    final String removePath = removePropertyIfExists(parent, prop.getName());
                    if ( removePath != null ) {
                        changes.add(Modification.onDeleted(removePath));
                    }
                } else {
                    changes.add(Modification.onModified(
                                    parent.node.setProperty(prop.getName(), "").getPath()
                    ));
                }
            } else {
                parent.valueMap.put(prop.getName(), "");
                changes.add(Modification.onModified(parent.resource.getPath() + '/' + prop.getName()));
            }
        }
    }

    /**
     * Removes the property if it exists.
     */
    private void removeProperty(final Modifiable parent, RequestProperty prop)
    throws RepositoryException, PersistenceException {
        final String removePath = removePropertyIfExists(parent, prop.getName());
        if ( removePath != null ) {
            changes.add(Modification.onDeleted(removePath));
        }
    }

    /**
     * Removes the property if it exists and is single-valued.
     */
    private void removeIfSingleValueProperty(final Modifiable parent,
                    final RequestProperty prop)
    throws RepositoryException, PersistenceException {
        if (parent.valueMap.containsKey(prop.getName())) {
            if ( parent.node != null ) {
                if (!parent.node.getProperty(prop.getName()).getDefinition().isMultiple()) {
                    // the existing property is single-valued, so we have to delete it before setting the
                    // multi-value variation
                    final String removePath = removePropertyIfExists(parent, prop.getName());
                    if ( removePath != null ) {
                        changes.add(Modification.onDeleted(removePath));
                    }
                }
            } else {
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
    private boolean storeAsDate(final Modifiable parent, String name, String[] values, boolean multiValued, ValueFactory valFac)
                    throws RepositoryException, PersistenceException {
        if (multiValued) {
            final Value[] array = dateParser.parse(values, valFac);
            if (array != null) {
                if ( parent.node != null ) {
                    parent.node.setProperty(name, array);
                } else {
                    final Calendar[] dates = new Calendar[array.length];
                    int index = 0;
                    for(final Value v : array) {
                        dates[index] = v.getDate();
                        index++;
                    }
                    parent.valueMap.put(name, dates);
                }
                changes.add(Modification.onModified(parent.resource.getPath() + '/' + name));
                return true;
            }
        } else {
            if (values.length >= 1) {
                final Calendar c = dateParser.parse(values[0]);
                if (c != null) {
                    parent.valueMap.put(name, c);
                    changes.add(Modification.onModified(parent.resource.getPath() + '/' + name));

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
    private boolean storeAsReference(final Modifiable parent, String name, String[] values, int type, boolean multiValued, ValueFactory valFac)
    throws RepositoryException, PersistenceException {
        if ( parent.node == null ) {
            // TODO
            throw new PersistenceException("Resource " + parent.resource.getPath() + " does not support reference properties.");
        }
        if (multiValued) {
            Value[] array = referenceParser.parse(values, valFac, isWeakReference(type));
            if (array != null) {
                changes.add(Modification.onModified(
                                parent.node.setProperty(name, array).getPath()
                ));
                return true;
            }
        } else {
            if (values.length >= 1) {
                Value v = referenceParser.parse(values[0], valFac, isWeakReference(type));
                if (v != null) {
                    changes.add(Modification.onModified(
                                    parent.node.setProperty(name, v).getPath()
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
    private void store(final Modifiable parent,
                    final String name,
                    final String[] values,
                    final int type,
                    final boolean multiValued)
    throws RepositoryException, PersistenceException {
        // TODO
        if ( parent.node != null ) {
            Property p = null;

            if (multiValued) {
                if (type == PropertyType.UNDEFINED) {
                    p = parent.node.setProperty(name, values);
                } else {
                    p = parent.node.setProperty(name, values, type);
                }
            } else if (values.length >= 1) {
                if (type == PropertyType.UNDEFINED) {
                    p = parent.node.setProperty(name, values[0]);
                } else {
                    p = parent.node.setProperty(name, values[0], type);
                }
            }

            if (p != null) {
                changes.add(Modification.onModified(p.getPath()));
            }
        } else {
            if (multiValued) {
                parent.valueMap.put(name, values);
                changes.add(Modification.onModified(parent.resource.getPath() + '/' + name));
            } else if (values.length >= 1) {
                parent.valueMap.put(name, values[0]);
                changes.add(Modification.onModified(parent.resource.getPath() + '/' + name));
            }
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

    public final static class Modifiable {
        public Resource resource;
        public ModifiableValueMap valueMap;
        public Node node;
    }
}
