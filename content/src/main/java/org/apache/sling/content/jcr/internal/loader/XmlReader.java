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
package org.apache.sling.content.jcr.internal.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XmlReader implements NodeReader {

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

    private KXmlParser xmlParser;

    XmlReader() {
        this.xmlParser = new KXmlParser();
    }

    // ---------- XML content access -------------------------------------------

    public synchronized Node parse(InputStream ins) throws IOException {
        try {
            return this.parseInternal(ins);
        } catch (XmlPullParserException xppe) {
            throw (IOException) new IOException(xppe.getMessage()).initCause(xppe);
        }
    }

    private Node parseInternal(InputStream ins) throws IOException,
            XmlPullParserException {
        String currentElement = "<root>";
        LinkedList<String> elements = new LinkedList<String>();
        Node currentNode = null;
        LinkedList<Node> nodes = new LinkedList<Node>();
        StringBuffer contentBuffer = new StringBuffer();
        Property currentProperty = null;

        // set the parser input, use null encoding to force detection with
        // <?xml?>
        this.xmlParser.setInput(ins, null);

        int eventType = this.xmlParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {

                elements.add(currentElement);
                currentElement = this.xmlParser.getName();

                if (ELEM_PROPERTY.equals(currentElement)) {
                    currentProperty = new Property();
                } else if (ELEM_NODE.equals(currentElement)) {
                    if (currentNode != null) nodes.add(currentNode);
                    currentNode = new Node();
                }

            } else if (eventType == XmlPullParser.END_TAG) {

                String qName = this.xmlParser.getName();
                String content = contentBuffer.toString().trim();
                contentBuffer.delete(0, contentBuffer.length());

                if (ELEM_PROPERTY.equals(qName)) {
                    currentNode.addProperty(currentProperty);
                    currentProperty = null;

                } else if (ELEM_NAME.equals(qName)) {
                    if (currentProperty != null) {
                        currentProperty.setName(content);
                    } else if (currentNode != null) {
                        currentNode.setName(content);
                    }

                } else if (ELEM_VALUE.equals(qName)) {
                    if (currentProperty.isMultiValue()) {
                        currentProperty.addValue(content);
                    } else {
                        currentProperty.setValue(content);
                    }

                } else if (ELEM_VALUES.equals(qName)) {
                    currentProperty.addValue(null);
                    currentProperty.setValue(null);

                } else if (ELEM_TYPE.equals(qName)) {
                    currentProperty.setType(content);

                } else if (ELEM_NODE.equals(qName)) {
                    if (!nodes.isEmpty()) {
                        Node parent = nodes.removeLast();
                        parent.addChild(currentNode);
                        currentNode = parent;
                    }

                } else if (ELEM_PRIMARY_NODE_TYPE.equals(qName)) {
                    currentNode.setPrimaryNodeType(content);

                } else if (ELEM_MIXIN_NODE_TYPE.equals(qName)) {
                    currentNode.addMixinNodeType(content);
                }

                currentElement = elements.removeLast();

            } else if (eventType == XmlPullParser.TEXT) {
                contentBuffer.append(this.xmlParser.getText());
            }

            eventType = this.xmlParser.next();
        }

        return currentNode;
    }
}
