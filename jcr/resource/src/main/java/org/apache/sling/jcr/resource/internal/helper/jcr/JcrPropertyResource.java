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

import java.util.Calendar;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

public class JcrPropertyResource extends JcrItemResource {

    private final Property property;

    private final String resourceType;

    public JcrPropertyResource(ResourceResolver resourceResolver, String path,
            Property property) throws RepositoryException {
        super(resourceResolver, path);
        this.property = property;
        this.resourceType = getResourceTypeForNode(property.getParent())
            + "/" + property.getName();
    }

    public String getResourceType() {
        return resourceType;
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {

        // the property itself
        if (type == Property.class || type == Item.class) {
            return (AdapterType) property;
        }

        // the property value
        try {
            if (type == String.class) {
                return (AdapterType) getProperty().getString();
            } else if (type == Boolean.class) {
                return (AdapterType) new Boolean(getProperty().getBoolean());
            } else if (type == Long.class) {
                return (AdapterType) new Long(getProperty().getLong());
            } else if (type == Double.class) {
                return (AdapterType) new Double(getProperty().getDouble());
            } else if (type == Calendar.class) {
                return (AdapterType) getProperty().getDate();
            } else if (type == Value.class) {
                return (AdapterType) getProperty().getValue();
            } else if (type == Node.class) {
                return (AdapterType) getProperty().getNode();
            }
        } catch (ValueFormatException vfe) {
            // TODO: log
        } catch (RepositoryException re) {
            // TODO: log
        }

        // try to use adapter factories
        return super.adaptTo(type);
    }

    public Property getProperty() {
        return property;
    }

    public String toString() {
        return getClass().getSimpleName() + ", type=" + getResourceType()
            + ", path=" + getPath();
    }
}
