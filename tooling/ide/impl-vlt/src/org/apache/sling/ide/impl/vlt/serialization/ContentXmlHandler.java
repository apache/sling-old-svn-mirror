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
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;

import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.vault.util.DocViewNode;
import org.apache.jackrabbit.vault.util.DocViewProperty;
import org.apache.sling.ide.impl.vlt.Activator;
import org.apache.sling.ide.transport.ResourceProxy;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ContentXmlHandler extends DefaultHandler implements NamespaceResolver {

    private static final String JCR_ROOT = "jcr:root";
    private final ResourceProxy root;
    private final Deque<ResourceProxy> queue = new LinkedList<>();

    /** 
     * map containing fully qualified uris as keys and their defined prefixes as values
     */
    private final Map<String, String> uriPrefixMap;
    
    /**
     * the default name path resolver
     */
    private final DefaultNamePathResolver npResolver = new DefaultNamePathResolver(this);

    /**
     * all type hint classes in a map (key = type integer value)
     */
    private static final Map<Integer, TypeHint> TYPE_HINT_MAP;
    
    static {
        TYPE_HINT_MAP = new HashMap<>();
        for (TypeHint hint : EnumSet.allOf(TypeHint.class)) {
            TYPE_HINT_MAP.put(hint.propertyType, hint);
        }
    }

    public ContentXmlHandler(String rootResourcePath) {
        root = new ResourceProxy(rootResourcePath);
        uriPrefixMap = new HashMap<>();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        uriPrefixMap.put(uri, prefix);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        ResourceProxy current;
        // name is equal to label except for SNS
        String label = ISO9075.decode(qName);
        String name = label;
        // code mostly taken from {@link org.apache.jackrabbit.vault.fs.impl.io.DocViewSaxImporter}
        DocViewNode node;
        try {
            node = new DocViewNode(name, label, attributes, npResolver);
            
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

            for (Map.Entry<String, DocViewProperty> entry : node.props.entrySet()) {

                try {
                    Object typedValue = TypeHint.convertDocViewPropertyToTypedValue(entry.getValue());
                    
                    // unsupported
                    if (typedValue == null) {
                        continue;
                    }
                    current.addProperty(entry.getKey(), typedValue);
                } catch (Throwable t) {
                    Activator.getDefault().getPluginLogger().error("Could not parse property '" + entry.getValue().name, t);
                }
            }

            queue.add(current);
        } catch (NamespaceException e) {
            Activator.getDefault().getPluginLogger().error("Could not resolve a JCR namespace.", e);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        queue.removeLast();
    }

    public ResourceProxy getRoot() {
        return root;
    }
    
    /**
     * Each enum implements the {@link TypeHint#parseValues(String[], boolean)} in a way, that the String[] value is converted to the closest underlying type.
     */
    static enum TypeHint {
        UNDEFINED(PropertyType.UNDEFINED) {
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return STRING.parseValues(values, explicitMultiValue);
            }
        },
        STRING(PropertyType.STRING) {
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return values[0];
                } else {
                    return values;
                }
            }
        },
        BINARY(PropertyType.BINARY) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return null;
            }
        },
        BOOLEAN(PropertyType.BOOLEAN) {
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
        DATE(PropertyType.DATE) {
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
        DOUBLE(PropertyType.DOUBLE) {
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
        LONG(PropertyType.LONG) {
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
        DECIMAL(PropertyType.DECIMAL) {
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
        NAME(PropertyType.NAME) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return values[0];
                }
                return values;
            }
        },
        PATH(PropertyType.PATH) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return NAME.parseValues(values, explicitMultiValue);
            }
        },
        REFERENCE(PropertyType.REFERENCE) {
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
        WEAKREFERENCE(PropertyType.WEAKREFERENCE) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return REFERENCE.parseValues(values, explicitMultiValue);
            }
        },
        URI(PropertyType.URI) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                try {
                    if (values.length == 1 && !explicitMultiValue) {
                        return new java.net.URI(values[0]);
                    }
    
                    java.net.URI[] refs = new java.net.URI[values.length];
                    for (int i = 0; i < values.length; i++) {
                        String value = values[i];
                        refs[i] = new java.net.URI(value);
                    }
                    return refs;
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Given value cannot be converted to URI", e);
                }
            }
        };

        static Object convertDocViewPropertyToTypedValue(DocViewProperty property) {
            TypeHint hint = TYPE_HINT_MAP.get(property.type);
            if (hint == null) {
                throw new IllegalArgumentException("Unknown type value '" + property.type + "'");
            }
            return hint.parseValues(property.values, property.isMulti);
        }

        private final int propertyType;

        /**
         * 
         * @param propertyType one of type values being defined in {@link javax.jcr.PropertyType}
         */
        private TypeHint(int propertyType) {

            this.propertyType = propertyType;
        }

        abstract Object parseValues(String[] values, boolean explicitMultiValue);

    }

    /**
     * {@inheritDoc}
     */
    public String getURI(String prefix) throws NamespaceException {
        throw new UnsupportedOperationException("The method getUri is not implemented as this is not being used");
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix(String uri) throws NamespaceException {
       String prefix = uriPrefixMap.get(uri);
       if (prefix == null) {
           throw new NamespaceException("Could not find defined prefix for uri " + uri);
       }
       return prefix;
    }
}
