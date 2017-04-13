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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Sets a property on the given resource, in some cases with a specific type and
 * value. For example, "lastModified" with an empty value is stored as the
 * current Date.
 * Special handling might apply if the resource is backed by a JCR node.
 */
public class SlingPropertyValueHandler {

    /**
     * Defines a map of auto properties
     */
    private static final Map<String, AutoType> AUTO_PROPS = new HashMap<>();
    static {
        AUTO_PROPS.put("created", AutoType.CREATED);
        AUTO_PROPS.put("createdBy", AutoType.CREATED_BY);
        AUTO_PROPS.put(JcrConstants.JCR_CREATED, AutoType.CREATED);
        AUTO_PROPS.put("jcr:createdBy", AutoType.CREATED_BY);
        AUTO_PROPS.put("lastModified", AutoType.MODIFIED);
        AUTO_PROPS.put("lastModifiedBy", AutoType.MODIFIED_BY);
        AUTO_PROPS.put(JcrConstants.JCR_LASTMODIFIED, AutoType.MODIFIED);
        AUTO_PROPS.put("jcr:lastModifiedBy", AutoType.MODIFIED_BY);
    }

    /**
     * the post processor
     */
    private final List<Modification> changes;

    private final DateParser dateParser;

    private final JCRSupport jcrSupport;

    /**
     * current date for all properties in this request
     */
    private final Calendar now = Calendar.getInstance();

    /**
     * Constructs a property value handler
     */
    public SlingPropertyValueHandler(final DateParser dateParser,
            final JCRSupport jcrSupport,
            final List<Modification> changes) {
        this.dateParser = dateParser;
        this.jcrSupport = jcrSupport;
        this.changes = changes;
    }

    /** Return the AutoType for a given property name
     *  @return null if not found
     * */
    static AutoType getAutoType(String propertyName) {
        return AUTO_PROPS.get(propertyName);
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
     * @throws PersistenceException if a resource error occurs
     */
    public void setProperty(final Resource parent, final RequestProperty prop)
    throws PersistenceException {
        final Modifiable mod = new Modifiable();
        mod.resource = parent;
        mod.node = jcrSupport.getNode(parent);
        mod.valueMap = parent.adaptTo(ModifiableValueMap.class);
        if ( mod.valueMap == null ) {
            throw new PersistenceException("Resource at '" + parent.getPath() + "' is not modifiable.");
        }

        final String name = prop.getName();
        if (prop.providesValue()) {
            // if user provided a value, don't mess with it
            setPropertyAsIs(mod, prop);

        } else if (AUTO_PROPS.containsKey(name)) {
            // check if this is a JCR resource and check node type
            if ( this.jcrSupport.isPropertyProtectedOrNewAutoCreated(mod.node, name) ) {
                return;
            }

            // avoid collision with protected properties
            final boolean isNew = jcrSupport.isNewNode(mod.node);
            switch (getAutoType(name)) {
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
        } else {
            // no magic field, set value as provided
            setPropertyAsIs(mod, prop);
        }
    }

    /**
     * Sets the property to the given date
     * @param parent parent resource
     * @param name name of the property
     * @throws PersistenceException if a resource error occurs
     */
    private void setCurrentDate(final Modifiable parent, final String name)
    throws PersistenceException {
        removePropertyIfExists(parent, name);
        parent.valueMap.put(name, now);
        changes.add(Modification.onModified(parent.resource.getPath() + '/' + name));
    }

    /**
     * set property to the current User id
     * @param parent parent resource
     * @param name name of the property
     * @throws PersistenceException if a resource error occurs
     */
    private void setCurrentUser(final Modifiable parent, final String name)
    throws PersistenceException {
        removePropertyIfExists(parent, name);
        final String user = parent.resource.getResourceResolver().getUserID();
        parent.valueMap.put(name, user);
        changes.add(Modification.onModified(parent.resource.getPath() + '/' + name));
    }

    /**
     * Removes the property with the given name from the parent resource if it
     * exists and if it's not a mandatory property.
     *
     * @param parent the parent resource
     * @param name the name of the property to remove
     * @return path of the property that was removed or <code>null</code> if
     *         it was not removed
     * @throws PersistenceException if a repository error occurs.
     */
    private String removePropertyIfExists(final Modifiable parent, final String name)
    throws PersistenceException {
        if (parent.valueMap.containsKey(name) && !jcrSupport.isPropertyMandatory(parent.node, name)) {
            parent.valueMap.remove(name);
            return parent.resource.getPath() + '/' + name;
        }
        return null;
    }

    /**
     * set property without processing, except for type hints
     *
     * @param parent the parent resource
     * @param prop the request property
     * @throws PersistenceException if a resource error occurs.
     */
    private void setPropertyAsIs(final Modifiable parent, final RequestProperty prop)
    throws PersistenceException {

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

            final boolean multiValue = isMultiValue(parent, prop, values);
            final int type = getType(parent, prop);

            if (multiValue) {
                // converting single into multi value props requires deleting it first
                removeIfSingleValueProperty(parent, prop);
            }

            if (jcrSupport.hasSession(parent.resource.getResourceResolver())) {
                if (type == PropertyType.DATE) {
                    if (storeAsDate(parent, prop.getName(), values, multiValue)) {
                        return;
                    }
                } else if (isReferencePropertyType(type)) {
                    if (storeAsReference(parent, prop.getName(), values, type, multiValue)) {
                        return;
                    }
                }
            }

            store(parent, prop.getName(), values, type, multiValue);
        }
    }

    /**
     * Patches a multi-value property using add and remove operations per value.
     */
    private String[] patch(final Modifiable parent, String name, String[] values)
    throws PersistenceException {
        // we do not use a Set here, as we want to be very restrictive in our
        // actions and avoid touching elements that are not modified through the
        // add/remove patch operations; e.g. if the value "foo" occurs twice
        // in the existing array, and is not touched, afterwards there should
        // still be two times "foo" in the list, even if this is not a real set.
        List<String> oldValues = new ArrayList<>();

        if (parent.valueMap.containsKey(name)) {
            if ( parent.node != null && !jcrSupport.isPropertyMultiple(parent.node, name)) {

                // can only patch multi-value props
                return null;
            }

            final String[] setValues = parent.valueMap.get(name, String[].class);
            if ( setValues != null ) {
                for(final String v : setValues) {
                    oldValues.add(v);
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
        return propertyType == PropertyType.REFERENCE || propertyType == PropertyType.WEAKREFERENCE;
    }

    /**
     * Returns the property type to use for the given property. This is defined
     * either by an explicit type hint in the request or simply the type of the
     * existing property.
     */
    private int getType(final Modifiable parent, RequestProperty prop)
    throws PersistenceException {
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
            final Integer jcrType = jcrSupport.getPropertyType(parent.node, prop.getName());
            if ( jcrType != null ) {
                type = jcrType;
            }
        }
        return type;
    }

    /**
     * Returns whether the property should be handled as multi-valued.
     */
    private boolean isMultiValue(final Modifiable parent, RequestProperty prop, String[] values)
    throws PersistenceException {
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
        final Object value = parent.valueMap.get(prop.getName());
        if ( parent.node != null ) {
            if ( value != null ) {
                return jcrSupport.isPropertyMultiple(parent.node, prop.getName());
            }
        } else {
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
    throws PersistenceException {
        if (parent.valueMap.containsKey(prop.getName())) {
            if ( jcrSupport.isPropertyMultiple(parent.node, prop.getName()) ) {
                // the existing property is multi-valued, so just delete it?
                final String removePath = removePropertyIfExists(parent, prop.getName());
                if ( removePath != null ) {
                    changes.add(Modification.onDeleted(removePath));
                }
            } else {
                changes.add(Modification.onModified(parent.resource.getPath() + '/' + prop.getName()));
            }
        }
    }

    /**
     * Removes the property if it exists.
     */
    private void removeProperty(final Modifiable parent, final RequestProperty prop)
    throws PersistenceException {
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
    throws PersistenceException {
        if (parent.valueMap.containsKey(prop.getName())) {
            if ( jcrSupport.isPropertyMultiple(parent.node, prop.getName()) ) {
                // do nothing, multi value
                return;
            }
            final String removePath = removePropertyIfExists(parent, prop.getName());
            if ( removePath != null ) {
                changes.add(Modification.onDeleted(removePath));
            }
        }
    }

    /**
     * Parses the given source strings and returns the respective Calendar value
     * instances. If no format matches for any of the sources
     * returns <code>null</code>.
     * <p/>
     *
     * @param sources date time source strings
     * @return Calendar value representations of the source or <code>null</code>
     */
    private Calendar[] parse(final String sources[]) {
        final Calendar ret[] = new Calendar[sources.length];
        for (int i=0; i< sources.length; i++) {
            final Calendar c = dateParser.parse(sources[i]);
            if (c == null) {
                return null;
            }
            ret[i] = c;
        }
        return ret;
    }


    /**
     * Stores property value(s) as date(s). Will parse the date(s) from the string
     * value(s) in the {@link RequestProperty}.
     *
     * @return true only if parsing was successful and the property was actually changed
     */
    private boolean storeAsDate(final Modifiable parent, String name, String[] values, boolean multiValued)
    throws PersistenceException {
        if (multiValued) {
            final Calendar[] array = parse(values);
            if (array != null) {
                parent.valueMap.put(name, array);
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
     * @return true only if parsing was successful and the property was actually changed
     */
    private boolean storeAsReference(final Modifiable parent,
            final String name,
            final String[] values,
            final int type,
            final boolean multiValued)
    throws PersistenceException {
        final Modification mod = this.jcrSupport.storeAsReference(parent.resource, parent.node, name, values, type, multiValued);
        return mod != null;
    }

    /**
     * Stores the property as string or via a string value, but with an explicit
     * type. Both multi-value or single-value.
     */
    private void store(final Modifiable parent,
            final String name,
            final String[] values,
            final int type,
            final boolean multiValued)
    throws PersistenceException {
        if ( parent.node != null && type != PropertyType.UNDEFINED ) {
            jcrSupport.setTypedProperty(parent.node, name, values, type, multiValued);

        } else {
            if (multiValued) {
                parent.valueMap.put(name, toJavaObject(values, type));
            } else if (values.length >= 1) {
                parent.valueMap.put(name, toJavaObject(values[0], type));
            }
        }
        changes.add(Modification.onModified(parent.resource.getPath() + '/' + name));
    }

    /** Converts a value */
    private static Object toJavaObject(final String value, final int type) {
        final boolean isEmpty = value == null || value.trim().length() == 0;
        switch (type) {
            case PropertyType.DECIMAL:
                return isEmpty ? BigDecimal.ZERO : new BigDecimal(value);
            case PropertyType.BOOLEAN:
                return isEmpty ? Boolean.FALSE : Boolean.valueOf(value);
            case PropertyType.DOUBLE:
                return isEmpty ? (double)0.0 : Double.valueOf(value);
            case PropertyType.LONG:
                return isEmpty ? 0 : Long.valueOf(value);
            default: // fallback
                return value;
        }
    }

    /** Converts a value */
    private static Object toJavaObject(final String values[], final int type) {
        final Object[] result = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null ) {
                result[i] = toJavaObject(values[i], type);
            }
        }
        return result;
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
        public Object node;
    }
}
