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
package org.apache.sling.ide.impl.vlt.serialization;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.ide.transport.ResourceProxy;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

// TODO - worth investigating whether we can properly use org.apache.jackrabbit.vault.util.DocViewProperty instead
public class ContentXmlHandler extends DefaultHandler {

    private static final String JCR_ROOT = "jcr:root";
    private final ResourceProxy root;
    private final Deque<ResourceProxy> queue = new LinkedList<>();

    public ContentXmlHandler(String rootResourcePath) {
        root = new ResourceProxy(rootResourcePath);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        ResourceProxy current;
        if (qName.equals(JCR_ROOT)) {
            current = root;
        } else {
            ResourceProxy parent = queue.peekLast();

            StringBuilder path = new StringBuilder(parent.getPath());
            if (path.charAt(path.length() - 1) != '/')
                path.append('/');
            path.append(qName);


            current = new ResourceProxy(ISO9075.decode(path.toString()));
            parent.addChild(current);
        }

        for (int i = 0; i < attributes.getLength(); i++) {

            String attributeQName = attributes.getQName(i);
            String value = attributes.getValue(i);
            Object typedValue = TypeHint.parsePossiblyTypedValue(value);
            
            // unsupported
            if (typedValue == null) {
                continue;
            }

            current.addProperty(attributeQName, typedValue);
        }

        queue.add(current);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        queue.removeLast();
    }

    public ResourceProxy getRoot() {
        return root;
    }
    
    // TODO - validate that this is comprehensive
    static enum TypeHint {
        BINARY("Binary") {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return null;
            }
        },
        BOOLEAN("Boolean") {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return Boolean.valueOf(values[0]);
                }

                Boolean[] ret = new Boolean[values.length];
                for (int i = 0; i < values.length; i++) {
                    ret[i] = Boolean.parseBoolean(values[i]);
                }
                return ret;

            }
        },
        DATE("Date") {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {

                if (values.length == 1 && !explicitMultiValue) {
                    return ISO8601.parse(values[0]);
                }

                Calendar[] ret = new Calendar[values.length];
                for (int i = 0; i < values.length; i++) {
                    ret[i] = ISO8601.parse(values[i]);
                }
                return ret;
            }
        },
        DOUBLE("Double") {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return Double.parseDouble(values[0]);
                }

                Double[] ret = new Double[values.length];
                for (int i = 0; i < values.length; i++) {
                    ret[i] = Double.parseDouble(values[i]);
                }
                return ret;
            }
        },
        LONG("Long") {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return Long.valueOf(values[0]);
                }
                
                Long[] ret = new Long[values.length];
                for ( int i =0 ; i < values.length; i++ ) {
                    ret[i] = Long.valueOf(values[i]);
                }
                return ret;
            }
        },
        DECIMAL("Decimal") {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return new BigDecimal(values[0]);
                }
                
                BigDecimal[] ret = new BigDecimal[values.length];
                for ( int i = 0; i < values.length; i++) {
                    ret[i] = new BigDecimal(values[i]);
                }
                return ret;
            }
        },
        NAME("Name") {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return values;
            }
        },
        PATH("Path") {

            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return NAME.parseValues(values, explicitMultiValue);
            }

        },
        REFERENCE("Reference") {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return UUID.fromString(values[0]);
                }

                UUID[] refs = new UUID[values.length];
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    refs[i] = UUID.fromString(value);
                }

                return refs;
            }

        },
        WEAKREFERENCE("WeakReference") {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return REFERENCE.parseValues(values, explicitMultiValue);
            }

        };

        static Object parsePossiblyTypedValue(String value) {

            boolean explicitMultiValue = false;
            String rawValue;
            int hintEnd = -1;
            
            if (value.isEmpty()) {
            	return value;
            }

            if (value.charAt(0) != '{') {
                rawValue = value;
            } else {
                hintEnd = value.indexOf('}');
                rawValue = value.substring(hintEnd + 1);
            }
            
            String[] values;

            // SLING-3609
            if (rawValue.length()>0 && rawValue.charAt(0) == '[') {

                if (rawValue.charAt(rawValue.length() - 1) != ']') {
                    throw new IllegalArgumentException("Invalid multi-valued property definition : '" + rawValue + "'");
                }

                String rawValues = rawValue.substring(1, rawValue.length() - 1);
                values = splitValues(rawValues);
                explicitMultiValue = true;
            } else {
                values = new String[] { rawValue };
            }

            // no hint -> string type
            if (hintEnd == -1) {
                unescape(values);
                if (values.length == 1 && !explicitMultiValue) {
                    return values[0];
                }
                return values;
            }

            String rawHint = value.substring(1, hintEnd);

            for (TypeHint hint : EnumSet.allOf(TypeHint.class)) {
                if (hint.rawHint.equals(rawHint)) {
                    return hint.parseValues(values, explicitMultiValue);
                }
            }

            throw new IllegalArgumentException("Unknown typeHint value '" + rawHint + "'");
        }

        private static String[] splitValues(String rawValues) {
            
            List<String> values = new ArrayList<>();
            String[] firstPass = rawValues.split(",");
            for ( int i = 0 ; i < firstPass.length; i++) {
                
                String val = firstPass[i];
                
                boolean trailingSlash = val.endsWith("\\");
                boolean moreEntries = i < firstPass.length;
                
                // special case where the comma is escaped, so this means that
                // two values should be joined
                if ( trailingSlash && moreEntries ) {
                    // re-establish the value, e.g. first\,second becomes a single first,second entry
                    values.add(val.substring(0, val.length() - 1) + "," + firstPass[i+1]);
                    // manually advance the iteration couter since we consumed the next entry
                    i++;
                    continue;
                }
              
                values.add(val);
            }
            
            return values.toArray(new String[values.size()]);
            
        }

        private static void unescape(String[] values) {

            for (int i = 0; i < values.length; i++) {
                if (values[i].length() > 0 && values[i].charAt(0) == '\\') {
                    values[i] = values[i].substring(1);
                }
            }
        }

        private final String rawHint;

        private TypeHint(String rawHint) {

            this.rawHint = rawHint;
        }

        abstract Object parseValues(String[] values, boolean explicitMultiValue);

    }
}
