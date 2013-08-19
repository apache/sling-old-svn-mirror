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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

public class AttributeResource extends AbstractResource {

    private final String path;

    private final ResourceResolver resourceResolver;

    private final ResourceMetadata metadata = new ResourceMetadata();

    private final MBeanAttributeInfo info;

    private final MBeanServer server;

    private final ObjectName on;

    public AttributeResource(final MBeanServer server,
                             final ObjectName on,
                             final ResourceResolver resolver, final String p, final MBeanAttributeInfo mai) {
        this.resourceResolver = resolver;
        this.path = p;
        this.info = mai;
        this.on = on;
        this.server = server;
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
        return "sling:mbeanattribute";
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
            return (AdapterType) new ValueMapDecorator(propMap);
        }
        return super.adaptTo(type);
    }

    private Map<String, Object> getPropertiesMap() {
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, this.getResourceType());
        if ( this.getResourceSuperType() != null ) {
            result.put("sling:resourceSuperType", this.getResourceSuperType());
        }

        if ( info.getDescription() != null ) {
            result.put("mbean:description", info.getDescription());
        }
        result.put("mbean:type", info.getType());

        try {
            final Object value = server.getAttribute(this.on, info.getName());
            if ( value != null ) {
                if ( value.getClass().isArray() ) {
                    final int length = Array.getLength(value);
                    final Object[] values = new Object[length];
                    for (int i = 0; i < length; i ++) {
                        final Object o = Array.get(value, i);
                        values[i] = convert(o);
                    }
                    result.put("mbean:value", values);
                } else if (value instanceof TabularData) {
                    // Nothing to do, value is child resource
                } else if (value instanceof CompositeData) {
                    // Nothing to do, value is child resource
                } else {
                    result.put("mbean:value", convert(value));
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

            return new MapResource(this.getResourceResolver(), this.getPath(), current);
        }
        return null;
    }

    private Map<String, Object> convertData() {
        try {
            final Object value = server.getAttribute(this.on, info.getName());

            if ( value instanceof TabularData ) {
                return convertObject((TabularData)value);
            } else if ( value instanceof CompositeData ) {
                return convertObject((CompositeData)value);
            }
        } catch (final Exception ignore) {
            // ignore and return null
        }
        return null;
    }

    private Map<String, Object> convertObject(final TabularData td) {
        final TabularType type = td.getTabularType();
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put("sling:resourceSuperType", "mbean:attributes");
        result.put("sling:resourceType", type.getTypeName());

        final Map<String, Map<String, Object>> rows = new LinkedHashMap<String, Map<String, Object>>();
        int index = 1;
        // TODO - use index values
        for(final CompositeData data : (Collection<CompositeData>)td.values()) {
            rows.put(String.valueOf(index), convertObject(data));
            index++;
        }
        result.put("mbean:value", rows);

        return result;
    }

    private Map<String, Object> convertObject(final CompositeData cd) {
        final CompositeType type = cd.getCompositeType();
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put("sling:resourceSuperType", "mbean:attributes");
        result.put("sling:resourceType", type.getTypeName());

        final Map<String, Object> attrMap = new TreeMap<String, Object>();
        attrMap.put("sling:resourceType", "mbean:attributes");
        result.put("mbean:attributes", attrMap);

        final Set<String> names = type.keySet();
        for(final String name : names) {
            final Map<String, Object> dataMap = new HashMap<String, Object>();
            attrMap.put(name, dataMap);
            dataMap.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, type.getType(name));
            dataMap.put("sling:resourceSuperType", "mbean:attributes");

            if ( type.getDescription() != null ) {
                dataMap.put("mbean:description", type.getDescription());
            }
            dataMap.put("mbean:type", type.getType(name));

            final Object value = cd.get(name);
            if ( value != null ) {
                if ( value.getClass().isArray() ) {
                    final int length = Array.getLength(value);
                    final Object[] values = new Object[length];
                    for (int i = 0; i < length; i ++) {
                        final Object o = Array.get(value, i);
                        values[i] = convert(o);
                    }
                    dataMap.put("mbean:value", values);
                } else if (value instanceof TabularData) {
                    dataMap.put("mbean:value", convertObject((TabularData)value));
                } else if (value instanceof CompositeData) {
                    dataMap.put("mbean:value", convertObject((CompositeData)value));
                } else {
                    dataMap.put("mbean:value", convert(value));
                }
            }
        }

        return result;
    }

    public Iterator<Resource> getChildren(String subPath) {
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
            final Iterator<Map.Entry<String, Object>> removeIter = current.entrySet().iterator();
            while ( removeIter.hasNext() ) {
                final Map.Entry<String, Object> c = removeIter.next();
                if ( !(c.getValue() instanceof Map) ) {
                    removeIter.remove();
                }
            }
            if ( current.size() == 0 ) {
                return null;
            }
            final Iterator<Map.Entry<String, Object>> childIter = current.entrySet().iterator();

            return new Iterator<Resource>() {

                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }

                public Resource next() {
                    final Map.Entry<String, Object> props = childIter.next();

                    return new MapResource(getResourceResolver(), getPath() + '/' + props.getKey(), (Map)props.getValue());
                }

                public boolean hasNext() {
                    return childIter.hasNext();
                }
            };
        }
        return null;
    }
}
