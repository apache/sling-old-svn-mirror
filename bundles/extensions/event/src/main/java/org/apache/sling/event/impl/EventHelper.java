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
package org.apache.sling.event.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.JobStatusProvider;
import org.apache.sling.event.impl.job.JobStatusNotifier;
import org.osgi.service.event.Event;


/**
 * Helper class defining some constants and utility methods.
 */
public abstract class EventHelper {

    /** The namespace prefix. */
    public static final String EVENT_PREFIX = "slingevent:";

    public static final String NODE_PROPERTY_TOPIC = "slingevent:topic";
    public static final String NODE_PROPERTY_APPLICATION = "slingevent:application";
    public static final String NODE_PROPERTY_CREATED = "slingevent:created";
    public static final String NODE_PROPERTY_PROPERTIES = "slingevent:properties";
    public static final String NODE_PROPERTY_PROCESSOR = "slingevent:processor";
    public static final String NODE_PROPERTY_JOBID = "slingevent:id";
    public static final String NODE_PROPERTY_FINISHED = "slingevent:finished";
    public static final String NODE_PROPERTY_TE_EXPRESSION = "slingevent:expression";
    public static final String NODE_PROPERTY_TE_DATE = "slingevent:date";
    public static final String NODE_PROPERTY_TE_PERIOD = "slingevent:period";

    public static final String EVENT_NODE_TYPE = "slingevent:Event";
    public static final String JOB_NODE_TYPE = "slingevent:Job";
    public static final String TIMED_EVENT_NODE_TYPE = "slingevent:TimedEvent";

    /** The nodetype for newly created intermediate folders */
    public static final String NODETYPE_FOLDER = "sling:Folder";

    /** The nodetype for newly created folders */
    public static final String NODETYPE_ORDERED_FOLDER = "sling:OrderedFolder";

    /** Allowed characters for a node name */
    private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz0123456789_,.-+*#!¤$%&()=[]?";
    /** Replacement characters for unallowed characters in a node name */
    private static final char REPLACEMENT_CHAR = '_';

    /** List of ignored properties to write to the repository. */
    private static final String[] IGNORE_PROPERTIES = new String[] {
        EventUtil.PROPERTY_DISTRIBUTE,
        EventUtil.PROPERTY_APPLICATION,
        JobStatusProvider.PROPERTY_EVENT_ID,
        JobStatusNotifier.CONTEXT_PROPERTY_NAME
    };

    /** List of ignored prefixes to read from the repository. */
    private static final String[] IGNORE_PREFIXES = new String[] {
        EventHelper.EVENT_PREFIX
    };

    /**
     * Filter the node name for not allowed characters and replace them.
     * @param nodeName The suggested node name.
     * @return The filtered node name.
     */
    public static String filter(final String nodeName) {
        final StringBuilder sb  = new StringBuilder();
        char lastAdded = 0;

        for(int i=0; i < nodeName.length(); i++) {
            final char c = nodeName.charAt(i);
            char toAdd = c;

            if (ALLOWED_CHARS.indexOf(c) < 0) {
                if (lastAdded == REPLACEMENT_CHAR) {
                    // do not add several _ in a row
                    continue;
                }
                toAdd = REPLACEMENT_CHAR;

            } else if(i == 0 && Character.isDigit(c)) {
                sb.append(REPLACEMENT_CHAR);
            }

            sb.append(toAdd);
            lastAdded = toAdd;
        }

        if (sb.length()==0) {
            sb.append(REPLACEMENT_CHAR);
        }

        return sb.toString();
    }

    /**
     * used for the md5
     */
    public static final char[] hexTable = "0123456789abcdef".toCharArray();

    /**
     * Calculate an MD5 hash of the string given using 'utf-8' encoding.
     *
     * @param data the data to encode
     * @return a hex encoded string of the md5 digested input
     */
    public static String md5(String data) {
        try {
            return digest("MD5", data.getBytes("utf-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("MD5 digest not available???");
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("UTF8 digest not available???");
        }
    }

    /**
     * Digest the plain string using the given algorithm.
     *
     * @param algorithm The alogrithm for the digest. This algorithm must be
     *                  supported by the MessageDigest class.
     * @param data      the data to digest with the given algorithm
     * @return The digested plain text String represented as Hex digits.
     * @throws java.security.NoSuchAlgorithmException if the desired algorithm is not supported by
     *                                  the MessageDigest class.
     */
    private static String digest(String algorithm, byte[] data)
    throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] digest = md.digest(data);
        StringBuilder res = new StringBuilder(digest.length * 2);
        for (int i = 0; i < digest.length; i++) {
            byte b = digest[i];
            res.append(hexTable[(b >> 4) & 15]);
            res.append(hexTable[b & 15]);
        }
        return res.toString();
    }

    /**
     * Check if this property should be ignored
     */
    private static boolean ignoreProperty(final String name) {
        for(final String prop : IGNORE_PROPERTIES) {
            if ( prop.equals(name) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add all java properties as properties to the node.
     * If the name and the value of a map entry can easily converted into
     * a repository property, it is directly added. All other java
     * properties are stored in one binary property.
     *
     * @param node The node where all properties are added to
     * @param event The event.
     * @throws RepositoryException
     */
    public static void writeEventProperties(final Node node,
                                            final Event event)
    throws RepositoryException {
        if ( event != null ) {
            final String[] propNames = event.getPropertyNames();
            if ( propNames != null && propNames.length > 0 ) {
                // check which props we can write directly and
                // which we need to write as a binary blob
                final List<String> propsAsBlob = new ArrayList<String>();

                for(final String name : propNames) {

                    if ( !ignoreProperty(name) ) {
                        // sanity check
                        final Object value = event.getProperty(name);
                        if ( value != null ) {
                            if ( !setProperty(name, value, node) ) {
                                propsAsBlob.add(name);
                            }
                        }
                    }
                }
                // write the remaining properties as a blob
                if ( propsAsBlob.size() > 0 ) {
                    try {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        final ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeInt(propsAsBlob.size());
                        for(final String propName : propsAsBlob) {
                            oos.writeObject(propName);
                            try {
                                oos.writeObject(event.getProperty(propName));
                            } catch (IOException ioe) {
                                throw new RepositoryException("Unable to serialize property " + propName, ioe);
                            }
                        }
                        oos.close();
                        node.setProperty(EventHelper.NODE_PROPERTY_PROPERTIES,
                                node.getSession().getValueFactory().createBinary(new ByteArrayInputStream(baos.toByteArray())));
                    } catch (IOException ioe) {
                        throw new RepositoryException("Unable to serialize event " + EventUtil.toString(event), ioe);
                    }
                }
            }
        }
    }

    /**
     * Read event properties from a repository node and create a property map (dictionary).
     * As the properties might contain serialized java objects, a class loader can be specified
     * for loading classes of the serialized java objects.
     * @throws RepositoryException
     * @throws ClassNotFoundException
     */
    public static Dictionary<String, Object> readEventProperties(final Node node,
                                                                 final ClassLoader objectClassLoader,
                                                                 final boolean forceLoad)
    throws RepositoryException, ClassNotFoundException {
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();

        // check the properties blob
        if ( node.hasProperty(EventHelper.NODE_PROPERTY_PROPERTIES) ) {
            try {
                final ObjectInputStream ois = new ObjectInputStream(node.getProperty(EventHelper.NODE_PROPERTY_PROPERTIES).getBinary().getStream(),
                        objectClassLoader);
                int length = ois.readInt();
                for(int i=0;i<length;i++) {
                    final String key = (String)ois.readObject();
                    final Object value = ois.readObject();
                    properties.put(key, value);
                }
            } catch (ClassNotFoundException cnfe) {
                if ( !forceLoad ) {
                    throw cnfe;
                }
            } catch (java.io.InvalidClassException ice) {
                if ( !forceLoad ) {
                    throw new ClassNotFoundException("Found invalid class.", ice);
                }
            } catch (IOException ioe) {
                if ( !forceLoad ) {
                    throw new RepositoryException("Unable to deserialize event properties.", ioe);
                }
            }
        }
        // now all properties that have been set directly
        final PropertyIterator pI = node.getProperties();
        while ( pI.hasNext() ) {
            final Property p = pI.nextProperty();
            boolean ignore = p.getName().startsWith("jcr:");
            if ( !ignore) {
                int index = 0;
                while ( !ignore && index < IGNORE_PREFIXES.length ) {
                    ignore = p.getName().startsWith(IGNORE_PREFIXES[index]);
                    index++;
                }
            }
            if ( !ignore ) {
                final String name = ISO9075.decode(p.getName());
                if ( p.getDefinition().isMultiple() ) {
                    final Value[] values = p.getValues();
                    if ( values.length > 0 ) {
                        // get first value
                        final Object firstObject = getPropertyValue(values[0]);
                        final Object[] array;
                        if ( firstObject instanceof Boolean ) {
                            array = new Boolean[values.length];
                        } else if ( firstObject instanceof Calendar ) {
                            array = new Calendar[values.length];
                        } else if ( firstObject instanceof Double ) {
                            array = new Double[values.length];
                        } else if ( firstObject instanceof Long ) {
                            array = new Long[values.length];
                        } else if ( firstObject instanceof BigDecimal) {
                            array = new BigDecimal[values.length];
                        } else {
                            array = new String[values.length];
                        }
                        array[0] = firstObject;
                        int index = 1;
                        while ( index < values.length ) {
                            array[index] = getPropertyValue(values[index]);
                            index++;
                        }
                        properties.put(name, array);
                    }
                } else {
                    final Value value = p.getValue();
                    final Object o = getPropertyValue(value);
                    properties.put(name, o);
                }
            }
        }
        return properties;
    }

    /**
     * Return the converted repository property name
     * @param name The java object property name
     * @return The converted name or null if not possible.
     */
    public static String getNodePropertyName(final String name) {
        // if name contains a colon, we can't set it as a property
        if ( name.indexOf(':') != -1 ) {
            return null;
        }
        return ISO9075.encode(name);
    }

    /**
     * Return the converted repository property value
     * @param valueFactory The value factory
     * @param eventValue The event value
     * @return The converted value or null if not possible
     */
    public static Value getNodePropertyValue(final ValueFactory valueFactory, final Object eventValue) {
        final Value val;
        if (eventValue instanceof Calendar) {
            val = valueFactory.createValue((Calendar)eventValue);
        } else if (eventValue instanceof Long) {
            val = valueFactory.createValue((Long)eventValue);
        } else if (eventValue instanceof Double) {
            val = valueFactory.createValue(((Double)eventValue).doubleValue());
        } else if (eventValue instanceof Boolean) {
            val = valueFactory.createValue((Boolean) eventValue);
        } else if (eventValue instanceof BigDecimal) {
            val = valueFactory.createValue((BigDecimal) eventValue);
        } else if (eventValue instanceof String) {
            val = valueFactory.createValue((String)eventValue);
        } else {
            val = null;
        }
        return val;
    }

    /**
     * Convert the value back to an object.
     * @param value
     * @return
     * @throws RepositoryException
     */
    private static Object getPropertyValue(final Value value)
    throws RepositoryException {
        final Object o;
        switch (value.getType()) {
            case PropertyType.BOOLEAN:
                o = value.getBoolean(); break;
            case PropertyType.DATE:
                o = value.getDate(); break;
            case PropertyType.DOUBLE:
                o = value.getDouble(); break;
            case PropertyType.LONG:
                o = value.getLong(); break;
            case PropertyType.STRING:
                o = value.getString(); break;
            case PropertyType.DECIMAL:
                o = value.getDecimal(); break;
            default: // this should never happen - we convert to a string...
                o = value.getString();
        }
        return o;
    }

    /**
     * Try to set the java property as a property of the node.
     * @param name
     * @param value
     * @param node
     * @return
     * @throws RepositoryException
     */
    private static boolean setProperty(String name, Object value, Node node)
    throws RepositoryException {
        final String propName = getNodePropertyName(name);
        if ( propName == null ) {
            return false;
        }
        final ValueFactory fac = node.getSession().getValueFactory();
        // check for multi value
        if ( value.getClass().isArray() ) {
            final Object[] array = (Object[])value;
            // now we try to convert each value
            // and check if all converted values have the same type
            final Value[] values = new Value[array.length];
            int index = 0;
            for(final Object v : array ) {
                values[index] = getNodePropertyValue(fac, v);
                if ( values[index] == null ) {
                    return false;
                }
                if ( index > 0 && !values[index-1].getClass().equals(values[index].getClass()) ) {
                    return false;
                }
                index++;
            }
            node.setProperty(propName, values);
            return true;
        }
        final Value val = getNodePropertyValue(fac, value);
        if ( val != null ) {
            node.setProperty(propName, val);
            return true;
        }
        return false;
    }

    /**
     * This is an extended version of the object input stream which uses the
     * thread context class loader.
     */
    private static class ObjectInputStream extends java.io.ObjectInputStream {

        private ClassLoader classloader;

        public ObjectInputStream(final InputStream in, final ClassLoader classLoader) throws IOException {
            super(in);
            this.classloader = classLoader;
        }

        /**
         * @see java.io.ObjectInputStream#resolveClass(java.io.ObjectStreamClass)
         */
        @Override
        protected Class<?> resolveClass(java.io.ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
            if ( this.classloader != null ) {
                return Class.forName(classDesc.getName(), true, this.classloader);
            }
            return super.resolveClass(classDesc);
        }
    }
}
