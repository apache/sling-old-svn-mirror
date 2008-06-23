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
package org.apache.sling.jcr.contentloader.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * This reader reads an xml file defining the content.
 * The xml format should have this format:
 * <node>
 *   <name>the name of the node</name>
 *   <primaryNodeType>type</primaryNodeType>
 *   <mixinNodeTypes>
 *     <mixinNodeType>mixtype1</mixinNodeType>
 *     <mixinNodeType>mixtype2</mixinNodeType>
 *   </mixingNodeTypes>
 *   <properties>
 *     <property>
 *       <name>propName</name>
 *       <value>propValue</value>
 *           or
 *       <values>
 *         <value/> for multi value properties
 *       </values>
 *       <type>propType</type>
 *     </property>
 *     <!-- more properties -->
 *   </properties>
 *   <nodes>
 *     <!-- child nodes -->
 *     <node>
 *       ..
 *     </node>
 *   </nodes>
 * </node>
 */
public class XmlReader implements ContentReader {

    /*
     * <node> <primaryNodeType>type</primaryNodeType> <mixinNodeTypes>
     * <mixinNodeType>mixtype1</mixinNodeType> <mixinNodeType>mixtype2</mixinNodeType>
     * </mixinNodeTypes> <properties> <property> <name>propName</name>
     * <value>propValue</value> <type>propType</type> </property> <!-- more
     * --> </properties> </node>
     */

    private static final String ELEM_NODE = "node";

    private static final String ELEM_PRIMARY_NODE_TYPE = "primaryNodeType";

    private static final String ELEM_MIXIN_NODE_TYPE = "mixinNodeType";

    private static final String ELEM_PROPERTY = "property";

    private static final String ELEM_NAME = "name";

    private static final String ELEM_VALUE = "value";

    private static final String ELEM_VALUES = "values";

    private static final String ELEM_TYPE = "type";

    static final ImportProvider PROVIDER = new ImportProvider() {
        private XmlReader xmlReader;

        public ContentReader getReader() throws IOException {
            if (xmlReader == null) {
                try {
                    xmlReader = new XmlReader();
                } catch (Throwable t) {
                    throw (IOException) new IOException(t.getMessage()).initCause(t);
                }
            }
            return xmlReader;
        }
    };

    private KXmlParser xmlParser;

    XmlReader() {
        this.xmlParser = new KXmlParser();
    }

    // ---------- XML content access -------------------------------------------


    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentReader#parse(java.io.InputStream, org.apache.sling.jcr.contentloader.internal.ContentCreator)
     */
    public synchronized void parse(InputStream ins, ContentCreator creator)
    throws IOException, RepositoryException {
        try {
            this.parseInternal(ins, creator);
        } catch (XmlPullParserException xppe) {
            throw (IOException) new IOException(xppe.getMessage()).initCause(xppe);
        }
    }

    private void parseInternal(InputStream ins, ContentCreator creator)
    throws IOException, XmlPullParserException, RepositoryException {
        final StringBuffer contentBuffer = new StringBuffer();

        // set the parser input, use null encoding to force detection with
        // <?xml?>
        this.xmlParser.setInput(ins, null);

        NodeDescription.SHARED.clear();
        PropertyDescription.SHARED.clear();

        NodeDescription currentNode = null;
        PropertyDescription currentProperty = null;
        String currentElement;

        int eventType = this.xmlParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {

                currentElement = this.xmlParser.getName();

                if (ELEM_PROPERTY.equals(currentElement)) {
                    currentNode = NodeDescription.create(currentNode, creator);
                    currentProperty = PropertyDescription.SHARED;
                } else if (ELEM_NODE.equals(currentElement)) {
                    currentNode = NodeDescription.create(currentNode, creator);
                    currentNode = NodeDescription.SHARED;
                }

            } else if (eventType == XmlPullParser.END_TAG) {

                String qName = this.xmlParser.getName();
                String content = contentBuffer.toString().trim();
                contentBuffer.delete(0, contentBuffer.length());

                if (ELEM_PROPERTY.equals(qName)) {
                    currentProperty = PropertyDescription.create(currentProperty, creator);

                } else if (ELEM_NAME.equals(qName)) {
                    if (currentProperty != null) {
                        currentProperty.name = content;
                    } else if (currentNode != null) {
                        currentNode.name = content;
                    }

                } else if (ELEM_VALUE.equals(qName)) {
                    currentProperty.addValue(content);

                } else if (ELEM_VALUES.equals(qName)) {
                    currentProperty.isMultiValue = true;

                } else if (ELEM_TYPE.equals(qName)) {
                    currentProperty.type = content;

                } else if (ELEM_NODE.equals(qName)) {
                    currentNode = NodeDescription.create(currentNode, creator);
                    creator.finishNode();

                } else if (ELEM_PRIMARY_NODE_TYPE.equals(qName)) {
                    if ( currentNode == null ) {
                        throw new IOException("Element is not allowed at this location: " + qName);
                    }
                    currentNode.primaryNodeType = content;

                } else if (ELEM_MIXIN_NODE_TYPE.equals(qName)) {
                    if ( currentNode == null ) {
                        throw new IOException("Element is not allowed at this location: " + qName);
                    }
                    currentNode.addMixinType(content);
                }

            } else if (eventType == XmlPullParser.TEXT) {
                contentBuffer.append(this.xmlParser.getText());
            }

            eventType = this.xmlParser.next();
        }
    }

    protected static final class NodeDescription {

        public static NodeDescription SHARED = new NodeDescription();

        public String name;
        public String primaryNodeType;
        public List<String> mixinTypes;

        public static NodeDescription create(NodeDescription desc, ContentCreator creator)
        throws RepositoryException {
            if ( desc != null ) {
                creator.createNode(desc.name, desc.primaryNodeType, desc.getMixinTypes());
                desc.clear();
            }
            return null;
        }

        public void addMixinType(String v) {
            if ( this.mixinTypes == null ) {
                this.mixinTypes = new ArrayList<String>();
            }
            this.mixinTypes.add(v);
        }


        private String[] getMixinTypes() {
            if ( this.mixinTypes == null || this.mixinTypes.size() == 0) {
                return null;
            }
            return mixinTypes.toArray(new String[this.mixinTypes.size()]);
        }

        private void clear() {
            this.name = null;
            this.primaryNodeType = null;
            if ( this.mixinTypes != null ) {
                this.mixinTypes.clear();
            }
        }
    }

    protected static final class PropertyDescription {

        public static PropertyDescription SHARED = new PropertyDescription();

        public static PropertyDescription create(PropertyDescription desc, ContentCreator creator)
        throws RepositoryException {
            int type = (desc.type == null ? PropertyType.STRING : PropertyType.valueFromName(desc.type));
            if ( desc.isMultiValue ) {
                creator.createProperty(desc.name, type, desc.getPropertyValues());
            } else {
                String value = null;
                if ( desc.values != null && desc.values.size() == 1 ) {
                    value = desc.values.get(0);
                }
                creator.createProperty(desc.name, type, value);
            }
            desc.clear();
            return null;
        }

        public String name;
        public String type;
        public List<String> values;
        public boolean isMultiValue;

        public void addValue(String v) {
            if ( this.values == null ) {
                this.values = new ArrayList<String>();
            }
            this.values.add(v);
        }

        private String[] getPropertyValues() {
            if ( this.values == null || this.values.size() == 0) {
                return null;
            }
            return values.toArray(new String[this.values.size()]);
        }

        private void clear() {
            this.name = null;
            this.type = null;
            if ( this.values != null ) {
                this.values.clear();
            }
            this.isMultiValue = false;
        }
    }
}
