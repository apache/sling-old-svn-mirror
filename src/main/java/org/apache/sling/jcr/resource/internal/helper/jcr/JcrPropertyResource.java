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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.sling.adapter.annotations.Adaptable;
import org.apache.sling.adapter.annotations.Adapter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Adaptable(adaptableClass = Resource.class, adapters = {
        @Adapter(value = { Item.class, Property.class, Value.class, String.class, Boolean.class, Long.class,
                Double.class, BigDecimal.class, Calendar.class, InputStream.class, Value[].class, String[].class,
                Boolean[].class, Long[].class, Double[].class, BigDecimal[].class }),
        @Adapter(value = Node.class, condition = "If the resource is a JcrPropertyResource and the property is a reference or weak reference property.") })
class JcrPropertyResource extends JcrItemResource { // this should be package private, see SLING-1414

    /** default log */
    private static final Logger LOGGER = LoggerFactory.getLogger(JcrPropertyResource.class);

    private final Property property;

    private final String resourceType;

    public JcrPropertyResource(final ResourceResolver resourceResolver,
                               final String path,
                               final Property property)
    throws RepositoryException {
        super(resourceResolver, path);
        this.property = property;
        this.resourceType = getResourceTypeForNode(property.getParent())
                + "/" + property.getName();
        if (PropertyType.BINARY != this.property.getType()) {
            this.getResourceMetadata().setContentType("text/plain");
            this.getResourceMetadata().setCharacterEncoding("UTF-8");
        }

        this.setContentLength(property);
    }

    public JcrPropertyResource(final ResourceResolver resourceResolver,
                               final Property property)
    throws RepositoryException {
        this(resourceResolver, property.getPath(), property);
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceSuperType() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {

        // the property itself
        if (type == Property.class || type == Item.class) {
            return (AdapterType) getProperty();
        }

        // the property value
        try {
            if (type == String.class) {
                return (AdapterType) getProperty().getString();

            } else if (type == Boolean.class) {
                return (AdapterType) Boolean.valueOf(getProperty().getBoolean());

            } else if (type == Long.class) {
                return (AdapterType) Long.valueOf(getProperty().getLong());

            } else if (type == Double.class) {
                return (AdapterType) new Double(getProperty().getDouble());

            } else if (type == BigDecimal.class) {
                return (AdapterType) getProperty().getDecimal();

            } else if (type == Calendar.class) {
                return (AdapterType) getProperty().getDate();

            } else if (type == Value.class) {
                return (AdapterType) getProperty().getValue();

            } else if (type == Node.class
                && (getProperty().getType() == PropertyType.REFERENCE ||
                    getProperty().getType() == PropertyType.WEAKREFERENCE)) {
                return (AdapterType) getProperty().getNode();

            } else if (type == InputStream.class) {
                return (AdapterType) getInputStream();

            } else if ( type == String[].class ) {
                if ( getProperty().getDefinition().isMultiple() ) {
                    final Value[] values = getProperty().getValues();
                    final String[] result = new String[values.length];
                    for(int i=0; i<values.length; i++) {
                        result[i] = values[i].getString();
                    }
                    return (AdapterType)result;
                }
                return (AdapterType)new String[] {getProperty().getString()};

            } else if (type == Boolean[].class) {
                if ( getProperty().getDefinition().isMultiple() ) {
                    final Value[] values = getProperty().getValues();
                    final Boolean[] result = new Boolean[values.length];
                    for(int i=0; i<values.length; i++) {
                        result[i] = values[i].getBoolean();
                    }
                    return (AdapterType)result;
                }
                return (AdapterType)new Boolean[] {getProperty().getBoolean()};

            } else if (type == Long[].class) {
                if ( getProperty().getDefinition().isMultiple() ) {
                    final Value[] values = getProperty().getValues();
                    final Long[] result = new Long[values.length];
                    for(int i=0; i<values.length; i++) {
                        result[i] = values[i].getLong();
                    }
                    return (AdapterType)result;
                }
                return (AdapterType)new Long[] {getProperty().getLong()};

            } else if (type == Double[].class) {
                if ( getProperty().getDefinition().isMultiple() ) {
                    final Value[] values = getProperty().getValues();
                    final Double[] result = new Double[values.length];
                    for(int i=0; i<values.length; i++) {
                        result[i] = values[i].getDouble();
                    }
                    return (AdapterType)result;
                }
                return (AdapterType)new Double[] {getProperty().getDouble()};

            } else if (type == BigDecimal[].class) {
                if ( getProperty().getDefinition().isMultiple() ) {
                    final Value[] values = getProperty().getValues();
                    final BigDecimal[] result = new BigDecimal[values.length];
                    for(int i=0; i<values.length; i++) {
                        result[i] = values[i].getDecimal();
                    }
                    return (AdapterType)result;
                }
                return (AdapterType)new BigDecimal[] {getProperty().getDecimal()};

            } else if (type == Calendar[].class) {
                if ( getProperty().getDefinition().isMultiple() ) {
                    final Value[] values = getProperty().getValues();
                    final Calendar[] result = new Calendar[values.length];
                    for(int i=0; i<values.length; i++) {
                        result[i] = values[i].getDate();
                    }
                    return (AdapterType)result;
                }
                return (AdapterType)new Calendar[] {getProperty().getDate()};

            } else if (type == Value[].class) {
                if ( getProperty().getDefinition().isMultiple() ) {
                    return (AdapterType)getProperty().getValues();
                }
                return (AdapterType)new Value[] {getProperty().getValue()};

            }

        } catch (ValueFormatException vfe) {
            LOGGER.debug("adaptTo: Problem accessing the property value of {}: {}",
                getPath(), vfe.getMessage());
            LOGGER.debug("adaptTo: Cause", vfe);

        } catch (RepositoryException re) {
            LOGGER.debug("adaptTo: Problem accessing the property " + getPath(), re);
        }

        // try to use adapter factories
        return super.adaptTo(type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", type=" + getResourceType()
            + ", path=" + getPath();
    }

    private Property getProperty() {
        return property;
    }

    private InputStream getInputStream() {
        Property prop = getProperty();

        try {
            return prop.getBinary().getStream();
        } catch (RepositoryException re) {
            LOGGER.error("getInputStream: Problem accessing the property "
                + getPath() + " stream", re);
        }

        // fall back to none in case of an error
        return null;
    }

    @Override
    Iterator<Resource> listJcrChildren() {
        return null;
    }

    @Override
	public boolean hasChildren() {
		return false;
	}
}
