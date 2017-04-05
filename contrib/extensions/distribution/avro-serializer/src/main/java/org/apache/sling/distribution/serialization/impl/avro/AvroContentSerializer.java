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
package org.apache.sling.distribution.serialization.impl.avro;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionExportFilter;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apache Avro based {@link DistributionContentSerializer}
 */
public class AvroContentSerializer implements DistributionContentSerializer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;
    private final Schema schema;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.sss+hh:mm");

    public AvroContentSerializer(String name) {
        try {
            schema = new Schema.Parser().parse(getClass().getResourceAsStream("/shallowresource.avsc"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.name = name;
    }

    @Override
    public void exportToStream(ResourceResolver resourceResolver, DistributionExportOptions options, OutputStream outputStream) throws DistributionException {

        DatumWriter<AvroShallowResource> datumWriter = new SpecificDatumWriter<AvroShallowResource>(AvroShallowResource.class);
        DataFileWriter<AvroShallowResource> writer = new DataFileWriter<AvroShallowResource>(datumWriter);
        try {
            writer.create(schema, outputStream);
        } catch (IOException e) {
            throw new DistributionException(e);
        }

        try {
            DistributionExportFilter filter = options.getFilter();
            for (DistributionExportFilter.TreeFilter treeFilter : filter.getNodeFilters()) {
                String path = treeFilter.getPath();
                Resource resource = resourceResolver.getResource(path);
                AvroShallowResource avroShallowResource = getAvroShallowResource(treeFilter, filter.getPropertyFilter(),
                        resource);
                writer.append(avroShallowResource);
            }
            outputStream.flush();
        } catch (Exception e) {
            throw new DistributionException(e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    @Override
    public void importFromStream(ResourceResolver resourceResolver, InputStream stream) throws DistributionException {
        try {
            byte[] bin = IOUtils.toByteArray(stream); // TODO : avoid byte[] conversion
            Collection<AvroShallowResource> avroShallowResources = readAvroResources(bin);
            for (AvroShallowResource ar : avroShallowResources) {
                persistResource(resourceResolver, ar);
            }
            resourceResolver.commit();
        } catch (Exception e) {
            throw new DistributionException(e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isRequestFiltering() {
        return false;
    }

    private AvroShallowResource getAvroShallowResource(DistributionExportFilter.TreeFilter nodeFilter,
                                                       DistributionExportFilter.TreeFilter propertyFilter,
                                                       Resource resource) throws IOException {
        AvroShallowResource avroShallowResource = new AvroShallowResource();
        avroShallowResource.setName("avro_" + System.nanoTime());
        avroShallowResource.setPath(resource.getPath());
        avroShallowResource.setResourceType(resource.getResourceType());
        ValueMap valueMap = resource.getValueMap();
        Map<CharSequence, Object> map = new HashMap<CharSequence, Object>();
        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            String property = entry.getKey();
            if (propertyFilter.matches(property)) {
                Object value = entry.getValue();
                if (value instanceof GregorianCalendar) {
                    value = dateFormat.format(((GregorianCalendar) value).getTime());
                } else if (value instanceof Object[]) {
                    Object[] ar = (Object[]) value;
                    value = Arrays.asList(ar);
                } else if (value instanceof InputStream) {
                    value = ByteBuffer.wrap(IOUtils.toByteArray(((InputStream) value)));
                }
                map.put(property, value);
            }
        }
        avroShallowResource.setValueMap(map);
        List<AvroShallowResource> children = new LinkedList<AvroShallowResource>();
        for (Resource child : resource.getChildren()) {
            if (nodeFilter.matches(child.getPath())) {
                children.add(getAvroShallowResource(nodeFilter, propertyFilter, child));
            }
        }
        avroShallowResource.setChildren(children);
        return avroShallowResource;
    }

    private Collection<AvroShallowResource> readAvroResources(byte[] bytes) throws IOException {
        DatumReader<AvroShallowResource> datumReader = new SpecificDatumReader<AvroShallowResource>(AvroShallowResource.class);
        DataFileReader<AvroShallowResource> dataFileReader = new DataFileReader<AvroShallowResource>(new SeekableByteArrayInput(bytes), datumReader);
        Collection<AvroShallowResource> avroResources = new LinkedList<AvroShallowResource>();
        try {
            for (AvroShallowResource avroResource : dataFileReader) {
                avroResources.add(avroResource);
            }
        } finally {
            dataFileReader.close();
        }
        return avroResources;
    }

    private void persistResource(@Nonnull ResourceResolver resourceResolver, AvroShallowResource r) throws PersistenceException {
        String path = r.getPath().toString().trim();
        String name = path.substring(path.lastIndexOf('/') + 1);
        String substring = path.substring(0, path.lastIndexOf('/'));
        String parentPath = substring.length() == 0 ? "/" : substring;
        Map<String, Object> map = new HashMap<String, Object>();
        Map<CharSequence, Object> valueMap = r.getValueMap();
        for (Map.Entry<CharSequence, Object> entry : valueMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof GenericData.Array) {
                GenericData.Array array = (GenericData.Array) value;
                String[] s = new String[array.size()];
                for (int i = 0; i < s.length; i++) {
                    Object gd = array.get(i);
                    s[i] = gd.toString();
                }
                value = s;
            } else if (value instanceof Utf8) {
                value = value.toString();
            } else if (value instanceof ByteBuffer) {
                byte[] bytes = ((ByteBuffer) value).array();
                value = new BufferedInputStream(new ByteArrayInputStream(bytes));
            }
            map.put(entry.getKey().toString(), value);
        }
        Resource existingResource = resourceResolver.getResource(path);
        if (existingResource != null) {
            resourceResolver.delete(existingResource);
        }
        Resource parent = resourceResolver.getResource(parentPath);
        if (parent == null) {
            parent = createParent(resourceResolver, parentPath);
        }
        Resource createdResource = resourceResolver.create(parent, name, map);
        log.debug("created resource {}", createdResource);
        for (AvroShallowResource child : r.getChildren()) {
            persistResource(createdResource.getResourceResolver(), child);
        }
    }

    private Resource createParent(ResourceResolver resourceResolver, String path) throws PersistenceException {
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        String name = path.substring(path.lastIndexOf('/') + 1);
        Resource parentResource = resourceResolver.getResource(parentPath);
        if (parentResource == null) {
            parentResource = createParent(resourceResolver, parentPath);
        }
        Map<String, Object> properties = new HashMap<String, Object>();
        return resourceResolver.create(parentResource, name, properties);
    }
}
