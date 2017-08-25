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
package org.apache.sling.bundleresource.impl;

import static org.apache.jackrabbit.JcrConstants.NT_FILE;
import static org.apache.jackrabbit.JcrConstants.NT_FOLDER;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Resource that wraps a Bundle entry */
public class BundleResource extends AbstractResource {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ResourceResolver resourceResolver;

    private final BundleResourceCache cache;

    private final MappedPath mappedPath;

    private final String path;

    private URL resourceUrl;

    private final ResourceMetadata metadata;

    private final ValueMap valueMap;

    public BundleResource(final ResourceResolver resourceResolver,
            final BundleResourceCache cache,
            final MappedPath mappedPath,
            final String resourcePath,
            final String propsPath,
            final boolean isFolder) {

        this.resourceResolver = resourceResolver;
        this.cache = cache;
        this.mappedPath = mappedPath;

        metadata = new ResourceMetadata();
        metadata.setResolutionPath(resourcePath);
        metadata.setCreationTime(this.cache.getBundle().getLastModified());
        metadata.setModificationTime(this.cache.getBundle().getLastModified());

        this.path = resourcePath;

        final Map<String, Object> properties = new HashMap<>();
        this.valueMap = new ValueMapDecorator(Collections.unmodifiableMap(properties));
        if (isFolder) {

            properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, NT_FOLDER);

        } else {

            properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, NT_FILE);

            try {
                final URL url = this.cache.getEntry(mappedPath.getEntryPath(resourcePath));
                if ( url != null ) {
                    metadata.setContentLength(url.openConnection().getContentLength());
                }
            } catch (final Exception e) {
                // don't care, we just have no content length
            }
        }

        if ( propsPath != null ) {
            try {
                final URL url = this.cache.getEntry(mappedPath.getEntryPath(propsPath));
                if (url != null) {
                    final JsonObject obj = Json.createReader(url.openStream()).readObject();
                    for(final Map.Entry<String, JsonValue> entry : obj.entrySet()) {
                        final Object value = getValue(entry.getValue());
                        if ( value != null ) {
                            properties.put(entry.getKey(), value);
                        }
                    }
                }
            } catch (final IOException ioe) {
                log.error(
                        "getInputStream: Cannot get input stream for " + mappedPath.getEntryPath(propsPath), ioe);
            }

        }
    }

    private static Object getValue(final JsonValue value) {
        switch ( value.getValueType() ) {
            // type NULL -> return null
            case NULL : return null;
            // type TRUE or FALSE -> return boolean
            case FALSE : return false;
            case TRUE : return true;
            // type String -> return String
            case STRING : return ((JsonString)value).getString();
            // type Number -> return long or double
            case NUMBER : final JsonNumber num = (JsonNumber)value;
                          if (num.isIntegral()) {
                               return num.longValue();
                          }
                          return num.doubleValue();
            // type ARRAY -> return JSON string
            case ARRAY : final StringWriter writer = new StringWriter();
                         Json.createWriter(writer).writeArray((JsonArray)value);
                         return writer.toString();
             // type OBJECT -> return JSON string
             case OBJECT : final StringWriter mapWriter = new StringWriter();
                           Json.createWriter(mapWriter).writeObject((JsonObject)value);
                           return mapWriter.toString();
        }
        return null;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getResourceType() {
        return this.valueMap.get(ResourceResolver.PROPERTY_RESOURCE_TYPE, String.class);
    }

    @Override
    public String getResourceSuperType() {
        return this.valueMap.get("sling:resourceSuperType", String.class);
    }

    @Override
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Type> Type adaptTo(Class<Type> type) {
        if (type == InputStream.class) {
            return (Type) getInputStream(); // unchecked cast
        } else if (type == URL.class) {
            return (Type) getURL(); // unchecked cast
        } else if (type == ValueMap.class) {
            return (Type) valueMap; // unchecked cast
        }

        // fall back to adapter factories
        return super.adaptTo(type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", type=" + getResourceType()
                + ", path=" + getPath();
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Returns a stream to the bundle entry if it is a file. Otherwise returns
     * <code>null</code>.
     */
    private InputStream getInputStream() {
        // implement this for files only
        if (isFile()) {
            try {
                URL url = getURL();
                if (url != null) {
                    return url.openStream();
                }
            } catch (IOException ioe) {
                log.error(
                        "getInputStream: Cannot get input stream for " + this, ioe);
            }
        }

        // otherwise there is no stream
        return null;
    }

    private URL getURL() {
        if (resourceUrl == null) {
            final URL url = this.cache.getEntry(mappedPath.getEntryPath(this.path));
            if ( url != null ) {
                try {
                    resourceUrl = new URL(BundleResourceURLStreamHandler.PROTOCOL, null,
                            -1, path, new BundleResourceURLStreamHandler(
                                    cache.getBundle(), mappedPath.getEntryPath(path)));
                } catch (MalformedURLException mue) {
                    log.error("getURL: Cannot get URL for " + this, mue);
                }
            }
        }

        return resourceUrl;
    }

    @Override
    public Iterator<Resource> listChildren() {
        return new BundleResourceIterator(this);
    }

    BundleResourceCache getBundle() {
        return cache;
    }

    MappedPath getMappedPath() {
        return mappedPath;
    }

    boolean isFile() {
        return NT_FILE.equals(getResourceType());
    }
}
