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
package org.apache.sling.jmx.provider.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import javax.management.MBeanAttributeInfo;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.DeepReadValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapDecorator;

public class AttributeResource extends AbstractResource {

    private final String path;

    private final ResourceResolver resourceResolver;

    private final ResourceMetadata metadata = new ResourceMetadata();

    private final MBeanAttributeInfo info;

    private final Object attrValue;

    private final AttributesResource parent;

    public AttributeResource(final ResourceResolver resolver,
                             final String path,
                             final MBeanAttributeInfo info,
                             final Object value,
                             final AttributesResource parent) {
        this.resourceResolver = resolver;
        this.path = path;
        this.info = info;
        this.attrValue = value;
        this.parent = parent;
    }

    @Override
    public Resource getParent() {
        return this.parent;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getPath()
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceType()
     */
    public String getResourceType() {
        return Constants.TYPE_ATTRIBUTE;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
     */
    public String getResourceSuperType() {
        return null;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
     */
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceResolver()
     */
    public ResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if ( type == ValueMap.class || type == Map.class ) {
            final Map<String, Object> propMap = this.getPropertiesMap();
            return (AdapterType) new DeepReadValueMapDecorator(this, new ValueMapDecorator(propMap));
        }
        return super.adaptTo(type);
    }

    private Map<String, Object> getPropertiesMap() {
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put(Constants.PROP_RESOURCE_TYPE, this.getResourceType());
        if ( this.getResourceSuperType() != null ) {
            result.put(Constants.PROP_RESOURCE_SUPER_TYPE, this.getResourceSuperType());
        }

        if ( info.getDescription() != null ) {
            result.put(Constants.PROP_DESCRIPTION, info.getDescription());
        }
        result.put(Constants.PROP_TYPE, info.getType());

        try {
            final Object value = attrValue;
            if ( value != null ) {
                if ( value.getClass().isArray() ) {
                    final int length = Array.getLength(value);
                    final Object[] values = new Object[length];
                    for (int i = 0; i < length; i ++) {
                        final Object o = Array.get(value, i);
                        values[i] = convert(o);
                    }
                    result.put(Constants.PROP_VALUE, values);
                } else if (value instanceof TabularData) {
                    // Nothing to do, value is child resource
                } else if (value instanceof CompositeData) {
                    // Nothing to do, value is child resource
                } else {
                    result.put(Constants.PROP_VALUE, convert(value));
                }
            }
        } catch (final Exception ignore) {
            // ignore, but put this as info
            result.put("mbean:exception", ignore.getMessage());
        }
        return result;
    }

    private Object convert(final Object value) {
        if ( value == null ) {
            return "";
        } else if ( value instanceof String ) {
            return value;
        } else if ( value instanceof Number ) {
            return value;
        } else if ( value instanceof Boolean ) {
            return value;
        } else if ( value instanceof Character ) {
            return value;
        }
        return value.toString();
    }

    public Resource getChildResource(final String subPath) {
        final Map<String, Object> childStructure = this.convertData();
        if ( childStructure != null ) {
            final String[] segments = subPath.split("/");
            Map<String, Object> current = childStructure;
            for(final String path : segments) {
                final Object child = current.get(path);
                if ( child == null ) {
                    return null;
                }
                if ( !(child instanceof Map) ) {
                    return null;
                }
                current = (Map<String, Object>)child;
            }

            return new MapResource(this.getResourceResolver(), this.getPath(), current, this);
        }
        return null;
    }

    private volatile Map<String, Object> convertedValue;

    private Map<String, Object> convertData() {
        if ( convertedValue == null ) {
            if ( attrValue instanceof TabularData ) {
                convertedValue = convertObject((TabularData)attrValue);
            } else if ( attrValue instanceof CompositeData ) {
                convertedValue = convertObject((CompositeData)attrValue);
            }
        }
        return convertedValue;
    }

    private Map<String, Object> convertObject(final TabularData td) {
        final TabularType type = td.getTabularType();
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put(Constants.PROP_RESOURCE_SUPER_TYPE, Constants.TYPE_ATTRIBUTES);
        result.put(Constants.PROP_RESOURCE_TYPE, type.getTypeName());

        final Map<String, Map<String, Object>> rows = new LinkedHashMap<String, Map<String, Object>>();
        int rowIndex = 1;
        @SuppressWarnings("unchecked")
        final List<CompositeData> values = new ArrayList<CompositeData>((Collection<CompositeData>)td.values());
        Collections.sort(values, new Comparator<CompositeData>() {

            public int compare(final CompositeData o1, final CompositeData o2) {
                for(final String name : type.getIndexNames()) {
                    final Object value1 = o1.get(name);
                    final Object value2 = o2.get(name);
                    final int result;
                    if ( value1 instanceof Comparable ) {
                        result = ((Comparable)value1).compareTo(value2);
                    } else {
                        result = value1.toString().compareTo(value2.toString());
                    }
                    if ( result != 0 ) {
                        return result;
                    }
                }
                return 0;
            }

        });
        for(final CompositeData data : values) {
            rows.put(String.valueOf(rowIndex), convertObject(data));
            rowIndex++;
        }
        result.put(Constants.RSRC_VALUE, rows);

        return result;
    }

    private Map<String, Object> convertObject(final CompositeData cd) {
        final CompositeType type = cd.getCompositeType();
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put(Constants.PROP_RESOURCE_SUPER_TYPE, Constants.TYPE_ATTRIBUTES);
        result.put(Constants.PROP_RESOURCE_TYPE, type.getTypeName());

        final Map<String, Object> attrMap = new TreeMap<String, Object>();
        attrMap.put(Constants.PROP_RESOURCE_TYPE, Constants.TYPE_ATTRIBUTES);
        result.put(Constants.RSRC_ATTRIBUTES, attrMap);

        final Set<String> names = type.keySet();
        for(final String name : names) {
            final Map<String, Object> dataMap = new HashMap<String, Object>();
            attrMap.put(name, dataMap);
            dataMap.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, type.getType(name));
            dataMap.put(Constants.PROP_RESOURCE_SUPER_TYPE, Constants.TYPE_ATTRIBUTE);

            if ( type.getDescription() != null ) {
                dataMap.put(Constants.PROP_DESCRIPTION, type.getDescription());
            }
            dataMap.put(Constants.PROP_TYPE, type.getType(name).getTypeName());

            final Object value = cd.get(name);
            if ( value != null ) {
                if ( value.getClass().isArray() ) {
                    final int length = Array.getLength(value);
                    final Object[] values = new Object[length];
                    for (int i = 0; i < length; i ++) {
                        final Object o = Array.get(value, i);
                        values[i] = convert(o);
                    }
                    dataMap.put(Constants.PROP_VALUE, values);
                } else if (value instanceof TabularData) {
                    dataMap.put(Constants.RSRC_VALUE, convertObject((TabularData)value));
                } else if (value instanceof CompositeData) {
                    dataMap.put(Constants.RSRC_VALUE, convertObject((CompositeData)value));
                } else {
                    dataMap.put(Constants.PROP_VALUE, convert(value));
                }
            }
        }

        return result;
    }

    public Iterator<Resource> getChildren(final String parentPath, final String subPath) {
        final Map<String, Object> childStructure = this.convertData();
        if ( childStructure != null ) {
            Map<String, Object> current = childStructure;
            if ( subPath != null ) {
                final String[] segments = subPath.split("/");
                for(final String path : segments) {
                    final Object child = current.get(path);
                    if ( child == null ) {
                        return null;
                    }
                    if ( !(child instanceof Map) ) {
                        return null;
                    }
                    current = (Map<String, Object>)child;
                }
            }
            if ( current.size() == 0 ) {
                return null;
            }
            final Iterator<Map.Entry<String, Object>> childIter = current.entrySet().iterator();

            return new Iterator<Resource>() {

                private Map.Entry<String, Object> next = this.seek();

                private Map.Entry<String, Object> seek() {
                    while ( childIter.hasNext() ) {
                        final Map.Entry<String, Object> c = childIter.next();
                        if ( c.getValue() instanceof Map ) {
                            return c;
                        }
                    }
                    return null;
                }

                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }

                public Resource next() {
                    final Map.Entry<String, Object> props = next;
                    if ( props == null ) {
                        throw new NoSuchElementException();
                    }
                    next = seek();
                    return new MapResource(getResourceResolver(), parentPath + '/' + props.getKey(), (Map)props.getValue(), AttributeResource.this);
                }

                public boolean hasNext() {
                    return next != null;
                }
            };
        }
        return null;
    }
}
