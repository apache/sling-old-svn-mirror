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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.util.ISO8601;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

// TODO - worth investigating whether we can properly use org.apache.jackrabbit.vault.util.DocViewProperty instead
public class ContentXmlHandler extends DefaultHandler {

    private final Map<String, Object> properties = new HashMap<String, Object>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (!qName.equals("jcr:root")) {
            return;
        }

        for (int i = 0; i < attributes.getLength(); i++) {

            String attributeQName = attributes.getQName(i);
            String value = attributes.getValue(i);
            Object typedValue = TypeHint.parsePossiblyTypedValue(value);
            
            properties.put(attributeQName, typedValue);
        }
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
    
    // TODO - validate that this is comprehensive
    static enum TypeHint {
        BOOLEAN("Boolean") {
            @Override
            Object parseValue(String rawValue) {
                return Boolean.valueOf(rawValue);
            }
        },
        DATE("Date") {
            @Override
            Object parseValue(String rawValue) {
                return ISO8601.parse(rawValue);
            }
        },
        DOUBLE("Double") {
            @Override
            Object parseValue(String rawValue) {
                return Double.parseDouble(rawValue);
            }
        },
        LONG("Long") {
            @Override
            Object parseValue(String rawValue) {
                return Long.valueOf(rawValue);
            }
        },
        DECIMAL("Decimal") {
            @Override
            Object parseValue(String rawValue) {
                return new BigDecimal(rawValue);
            }
        };

        static Object parsePossiblyTypedValue(String value) {

            if (value.charAt(0) != '{') {
                return value;
            }

            int hintEnd = value.indexOf('}');

            String rawHint = value.substring(1, hintEnd);

            for (TypeHint hint : EnumSet.allOf(TypeHint.class)) {
                if (hint.rawHint.equals(rawHint)) {
                    return hint.parseValue(value.substring(hintEnd + 1));
                }
            }

            throw new IllegalArgumentException("Unknown typeHint value '" + rawHint + "'");
        }

        private final String rawHint;

        private TypeHint(String rawHint) {

            this.rawHint = rawHint;
        }

        abstract Object parseValue(String rawValue);

    }
}
