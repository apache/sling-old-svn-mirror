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
package org.apache.sling.jcr.ocm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableMap;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManageableMapImpl;


/**
 * The <code>DefaultMappedObject</code> is used by the JCR based resource
 * manager implemented by this bundle as a default to map JCR nodes. This class
 * has the following features:
 * <ul>
 * <li>This object may be loaded from any existing node.
 * <li>If inserting a new instance of this class into the repository a node of
 * type <code>nt:unstructured</code> without any mixin types is created
 * <li>All non-protected properties are simply read and may be accessed in the
 * Java standard <code>Map</code> style using the {@link #get(Object)} and
 * {@link #put(String, Object)} methods.
 * <li>Storing the object back to the repository just writes the map contents
 * into the properties. Care must be taken to obey the node type restrictions if
 * setting properties, otherwise storing back may fail.
 * <li>Single-value properties are loaded and stored as scalar instances
 * according to the mapping below. Multi-value properties are represented by
 * this object as <code>java.util.List</code> instances.
 * </ul>
 * <p>
 * <b>Reserved Properties</b>
 * <p>
 * The following is a list of reserved properties and their meanings. These
 * properties should not be modified through the getters and setters.
 * <dl>
 * <dt><code>path</code>
 * <dd>The path of the node underlying this instance.
 * <dt><code>primaryType</code>
 * <dd>The primary node type of the node underlying this instance. This
 * property is read-only. Any modifications will not be stored in the node (of
 * course). For newly created objects, this property is not used to indicate the
 * desired node type.
 * <dt><code>mixinTypes</code>
 * <dd>The list of mixin node types of the node underlying this instance. This
 * property is read-only. Any modifications will not be stored in the node. For
 * newly created objects, this property is not used to indicate the desired
 * mixin node types.
 * <dt><code>properties</code>
 * <dd>The map representing the properties of this content object itself.
 * </dl>
 * <p>
 * <b>Mapping JCR Property types</b> <table border="1" cellspacing="0"
 * cellpadding="3">
 * <tr>
 * <th colspan="2">Property type to Java types</th>
 * </tr>
 * <tr>
 * <th>JCR Type</th>
 * <th>Java Type</th>
 * </tr>
 * <tr>
 * <td>STRING</td>
 * <td>String</td>
 * </tr>
 * <tr>
 * <td>DATE</td>
 * <td>String</td>
 * </tr>
 * <tr>
 * <td>BINARY</td>
 * <td>java.io.InputStream</td>
 * </tr>
 * <tr>
 * <td>DOUBLE</td>
 * <td>java.lang.Double</td>
 * </tr>
 * <tr>
 * <td>LONG</td>
 * <td>java.lang.Long</td>
 * </tr>
 * <tr>
 * <td>BOOLEAN</td>
 * <td>java.lang.Boolean</td>
 * </tr>
 * <tr>
 * <td>NAME</td>
 * <td>String</td>
 * </tr>
 * <tr>
 * <td>PATH</td>
 * <td>String</td>
 * </tr>
 * <tr>
 * <td>REFERENCE</td>
 * <td>String</td>
 * </tr>
 * <tr>
 * <th colspan="2">Java type to Property type</th>
 * </tr>
 * <tr>
 * <th>Java Type</th>
 * <th>JCR Type</th>
 * </tr>
 * <tr>
 * <td>String</td>
 * <td>STRING</td>
 * </tr>
 * <tr>
 * <td>java.util.Date</td>
 * <td>DATE</td>
 * </tr>
 * <tr>
 * <td>java.util.Calendar</td>
 * <td>DATE</td>
 * </tr>
 * <tr>
 * <td>java.util.GregorianCalendar</td>
 * <td>DATE</td>
 * </tr>
 * <tr>
 * <td>java.io.InputStream</td>
 * <td>BINARY</td>
 * </tr>
 * <tr>
 * <td>java.lang.Double</td>
 * <td>DOUBLE</td>
 * </tr>
 * <tr>
 * <td>java.lang.Long</td>
 * <td>LONG</td>
 * </tr>
 * <tr>
 * <td>java.lang.Integer</td>
 * <td>LONG</td>
 * </tr>
 * <tr>
 * <td>java.lang.Boolean</td>
 * <td>BOOLEAN</td>
 * </tr>
 * </table>
 *
 * @ocm.mapped discriminator="false" extend=""
 */
public class DefaultMappedObject extends HashMap<String, Object> {

    /*
     * Implementation Note: As this class extends the HashMap class it
     * implements the Map interface and therefore the Commons Beanutils library
     * used by the Jackrabbit JCR Mapper will directly access the map getter and
     * setter methods to get and set properties instead of checking for specific
     * getter and setter methods. For this reason the get() and put() methods
     * have been overwritten here and check for the specially mapped property
     * "properties" to call the respective specific getter and setter methods.
     * As a consequence of this situation, no client properties of the name
     * "properties" may currently be used. Also the "properties" property is not
     * actually stored in the hash map and will not be iterated over. But the
     * check for existence (containsKey) will also consider the "properties"
     * property.
     */

    /**
     * The name of the <i>path</i> object property.
     * <p>
     * See the implementation notes above regarding this property.
     */
    private static final String FIELD_PATH = "path";

    /**
     * The name of the <i>properties</i> object property.
     * <p>
     * See the implementation notes above regarding this property.
     */
    private static final String FIELD_PROPERTIES = "properties";

    /**
     * The name of the <i>primaryType</i> object property.
     * <p>
     * See the implementation notes above regarding this property.
     */
    private static final String FIELD_PRIMARY_TYPE = "primaryType";

    /**
     * The name of the <i>mixinTypes</i> object property.
     * <p>
     * See the implementation notes above regarding this property.
     */
    private static final String FIELD_MIXIN_TYPES = "mixinTypes";

    /**
     * Returns the value of the indexed property.
     * <p>
     * For the special property <code>properties</code> the
     * {@link #getProperties()} method is called to return a copy of this map
     * instead of looking in the map itself.
     *
     * @param key The index of the property to return.
     * @return the indicated property or <code>null</code> if the map does not
     *         contain it.
     */
    public Object get(String key) {
        // handle properties as if they were map elements
        // due to an inconsistency in the BeanUtils 1.5 through 1.7.0
        // versions. Version 1.7.1 will fix this again, though ...
        if (FIELD_PROPERTIES.equals(key)) {
            return getProperties();
        }

        return super.get(key);
    }

    /**
     * Sets the value of the indexed property.
     * <p>
     * For the special property <code>properties</code> the
     * {@link #setProperties(ManageableMap)} method is called to insert all
     * elements of the <code>value</code> which must be a <code>Map</code>
     * into this map.
     *
     * @param key The index of the property to set
     * @param value The value of the property to set.
     * @return The former value of the property or <code>null</code> if the
     *         property is new.
     * @throws ClassCastException If the type of the <code>value</code> is
     *             <code>ManagedHashMap</code> for the <code>properties</code>
     *             property.
     */
    public Object put(String key, Object value) {
        // handle properties as if they were map elements
        // due to an inconsistency in the BeanUtils 1.5 through 1.7.0
        // versions. Version 1.7.1 will fix this again, though ...
        if (FIELD_PROPERTIES.equals(key)) {
            Object old = getProperties();
            setProperties((ManageableMap) value);
            return old;
        }

        return super.put(key, value);
    }

    /**
     * Returns <code>true</code> if this map contains the named property.
     */
    public boolean containsKey(String key) {
        return FIELD_PROPERTIES.equals(key) || super.containsKey(key);
    }

    // ---------- OCM support -------------------------------------------------

    /**
     * Sets the path of this mapped object.
     *
     * @ocm.field path="true"
     */
    public void setPath(String path) {
        if (path != null) {
            put(FIELD_PATH, path);
        }
    }

    /**
     * Returns the path of this mapped object.
     */
    public String getPath() {
        return (String) get(FIELD_PATH);
    }

    /**
     * Sets the properties of this content object as read from the properties of
     * the repository node.
     * <p>
     * This method copies the properties of the <code>contents</code> map into
     * this map instance.
     *
     * @ocm.collection jcrName="*"
     *                 collectionConverter="org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ResidualPropertiesCollectionConverterImpl"
     */
    @SuppressWarnings("unchecked")
    public void setProperties(ManageableMap contents) {
        if (contents != null) {
            putAll((Map<String, Object>) contents.getObjects()); // unchecked cast
        }
    }

    /**
     * Returns the properties of this content to be written to the properties of
     * the repository node.
     * <p>
     * The contents of the returned map replace all non-protected properties of
     * the repository. That is, existing properties not contained in the map
     * will be removed.
     * <p>
     * This method returns a new instance of a <code>ManagedHashMap</code>
     * containing a copy of the current properties on each call.
     */
    public ManageableMap getProperties() {
        // a copy of this map with the special properties removed
        HashMap<String, Object> properties = new HashMap<String, Object>(this);
        properties.remove(FIELD_PATH);
        properties.remove(FIELD_PRIMARY_TYPE);
        properties.remove(FIELD_MIXIN_TYPES);
        return new ManageableMapImpl(properties);
    }

    /**
     * Sets the primary node type of the underlying node.
     * <p>
     * This method should only be called by the Graffito Mapper to set the type
     * read from the node.
     *
     * @ocm.field jcrName="jcr:primaryType" autoUpdate="false"
     *            autoInsert="false"
     */
    public void setPrimaryType(String type) {
        put(FIELD_PRIMARY_TYPE, type);
    }

    /**
     * Returns the primary node type of the underlying node or <code>null</code>
     * for new objects.
     */
    public String getPrimaryType() {
        return (String) get(FIELD_PRIMARY_TYPE);
    }

    /**
     * Sets the mixin node types of the underlying node.
     * <p>
     * This method should only be called by the Graffito Mapper to set the types
     * read from the node.
     *
     * @ocm.collection jcrName="jcr:mixinTypes"
     *                 elementClassName="java.lang.String"
     *                 collectionConverter="org.apache.jackrabbit.ocm.manager.collectionconverter.impl.MultiValueCollectionConverterImpl"
     *                 autoUpdate="false" autoInsert="false"
     */
    public void setMixinTypes(ArrayList<String> mixinTypes) {
        if (mixinTypes != null) {
            put(FIELD_MIXIN_TYPES, new ArrayList<String>(mixinTypes));
        }
    }

    /**
     * Returns a list of the mixin node typse of the underlying node or
     * <code>null</code> for new objects or if the node has no mixin node
     * types.
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getMixinTypes() {
        List<String> types = (List<String>) get(FIELD_MIXIN_TYPES); // unchecked cast
        return (types != null) ? new ArrayList<String>(types) : null;
    }
}
