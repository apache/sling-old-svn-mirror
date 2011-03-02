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
package org.apache.sling.installer.provider.jcr.impl;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * The <code>JcrUtil</code> class provides helper methods used
 * throughout this bundle.
 */
public abstract class JcrUtil {

    private static final String FOLDER_NODE_TYPE = "sling:Folder";

    /**
     * Creates a {@link javax.jcr.Value JCR Value} for the given object with
     * the given Session.
     * Selects the the {@link javax.jcr.PropertyType PropertyType} according
     * the instance of the object's Class
     *
     * @param value object
     * @param session to create value for
     * @return the value or null if not convertible to a valid PropertyType
     * @throws RepositoryException in case of error, accessing the Repository
     */
    public static Value createValue(Object value, Session session)
            throws RepositoryException {
        Value val;
        ValueFactory fac = session.getValueFactory();
        if(value instanceof Calendar) {
            val = fac.createValue((Calendar)value);
        } else if (value instanceof InputStream) {
            val = fac.createValue((InputStream)value);
        } else if (value instanceof Node) {
            val = fac.createValue((Node)value);
        } else if (value instanceof Long) {
            val = fac.createValue((Long)value);
        } else if (value instanceof Integer) {
            val = fac.createValue(((Integer)value));
        } else if (value instanceof Number) {
            val = fac.createValue(((Number)value).doubleValue());
        } else if (value instanceof Boolean) {
            val = fac.createValue((Boolean) value);
        } else if ( value instanceof String ){
            val = fac.createValue((String)value);
        } else {
            val = null;
        }
        return val;
    }

    /**
     * Sets the value of the property.
     * Selects the {@link javax.jcr.PropertyType PropertyType} according
     * to the instance of the object's class.
     * @param node         The node where the property will be set on.
     * @param propertyName The name of the property.
     * @param propertyValue The value for the property.
     */
    public static void setProperty(final Node node,
                                   final String propertyName,
                                   final Object propertyValue)
    throws RepositoryException {
        if ( propertyValue == null ) {
            node.setProperty(propertyName, (String)null);
        } else if ( propertyValue.getClass().isArray() ) {
            final Object[] values = (Object[])propertyValue;
            final Value[] setValues = new Value[values.length];
            for(int i=0; i<values.length; i++) {
                setValues[i] = createValue(values[i], node.getSession());
            }
            node.setProperty(propertyName, setValues);
        } else {
            node.setProperty(propertyName, createValue(propertyValue, node.getSession()));
        }
    }

    /**
     * Creates or gets the {@link javax.jcr.Node Node} at the given Path.
     *
     * @param session The session to use for node creation
     * @param absolutePath absolute node path
     * @param nodeType to use for creation of the final node
     * @return the Node at path
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Node createPath(final Session session,
                                  final String absolutePath,
                                  final String nodeType)
    throws RepositoryException {
        final Node parentNode = session.getRootNode();
        String relativePath = absolutePath.substring(1);
        if (!parentNode.hasNode(relativePath)) {
            Node node = parentNode;
            int pos = relativePath.lastIndexOf('/');
            if ( pos != -1 ) {
                final StringTokenizer st = new StringTokenizer(relativePath.substring(0, pos), "/");
                while ( st.hasMoreTokens() ) {
                    final String token = st.nextToken();
                    if ( !node.hasNode(token) ) {
                        try {
                            node.addNode(token, FOLDER_NODE_TYPE);
                        } catch (RepositoryException re) {
                            // we ignore this as this folder might be created from a different task
                            node.refresh(false);
                        }
                    }
                    node = node.getNode(token);
                }
                relativePath = relativePath.substring(pos + 1);
            }
            if ( !node.hasNode(relativePath) ) {
                node.addNode(relativePath, nodeType);
            }
            return node.getNode(relativePath);
        }
        return parentNode.getNode(relativePath);
    }

    /**
     * Remove all properties from the node.
     * All properties without a namespace are removed.
     * @param node The node
     * @throws RepositoryException
     */
    public static void removeAllProperties(final Node node)
    throws RepositoryException {
        final PropertyIterator pI = node.getProperties();
        while ( pI.hasNext() ) {
            final javax.jcr.Property prop = pI.nextProperty();
            if ( !prop.getName().contains(":") ) {
                prop.remove();
            }
        }
    }

    public static void saveProperties(final Node configNode,
                                      final Dictionary<String, Object> dict)
    throws RepositoryException {
        final Enumeration<String> keys = dict.keys();
        while ( keys.hasMoreElements() ) {
            final String key = keys.nextElement();

            JcrUtil.setProperty(configNode, key, dict.get(key));
        }
    }
}
