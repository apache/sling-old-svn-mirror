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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apache Avro based {@link DistributionContentSerializer}
 */
public class AvroContentSerializer implements DistributionContentSerializer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;
    private final DataFileWriter<AvroShallowResource> dataFileWriter;
    private final Schema schema;
    private final Set<String> ignoredProperties;
    private final Set<String> ignoredNodeNames;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.sss+hh:mm");

    public AvroContentSerializer(String name) {
        DatumWriter<AvroShallowResource> datumWriter = new SpecificDatumWriter<AvroShallowResource>(AvroShallowResource.class);
        this.dataFileWriter = new DataFileWriter<AvroShallowResource>(datumWriter);
        try {
            schema = new Schema.Parser().parse(getClass().getResourceAsStream("/shallowresource.avsc"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Set<String> iProps = new HashSet<String>();
        iProps.add(JcrConstants.JCR_FROZENMIXINTYPES);
        iProps.add(JcrConstants.JCR_FROZENPRIMARYTYPE);
        iProps.add(JcrConstants.JCR_FROZENUUID);
        iProps.add(JcrConstants.JCR_VERSIONHISTORY);
        iProps.add(JcrConstants.JCR_BASEVERSION);
        iProps.add(JcrConstants.JCR_PREDECESSORS);
        iProps.add(JcrConstants.JCR_SUCCESSORS);
        iProps.add(JcrConstants.JCR_ISCHECKEDOUT);
        iProps.add(JcrConstants.JCR_UUID);
        ignoredProperties = Collections.unmodifiableSet(iProps);

        Set<String> iNames = new HashSet<String>();
        iNames.add("rep:policy");
        ignoredNodeNames = Collections.unmodifiableSet(iNames);
        this.name = name;
    }

    @Override
    public void exportToStream(ResourceResolver resourceResolver, DistributionRequest request, OutputStream outputStream) throws DistributionException {

        DataFileWriter<AvroShallowResource> writer;
        try {
            writer = dataFileWriter.create(schema, outputStream);
        } catch (IOException e) {
            throw new DistributionException(e);
        }

        try {

            for (String path : request.getPaths()) {
                Resource resource = resourceResolver.getResource(path);
                AvroShallowResource avroShallowResource = getAvroShallowResource(request.isDeep(path), path, resource);
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

    private AvroShallowResource getAvroShallowResource(boolean deep, String path, Resource resource) throws IOException {
        AvroShallowResource avroShallowResource = new AvroShallowResource();
        avroShallowResource.setName("avro_" + System.nanoTime());
        avroShallowResource.setPath(path);
        avroShallowResource.setResourceType(resource.getResourceType());
        ValueMap valueMap = resource.getValueMap();
        Map<CharSequence, Object> map = new HashMap<CharSequence, Object>();
        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            if (!ignoredProperties.contains(entry.getKey())) {
                Object value = entry.getValue();
                if (value instanceof GregorianCalendar) {
                    value = dateFormat.format(((GregorianCalendar) value).getTime());
                } else if (value instanceof Object[]) {
                    Object[] ar = (Object[]) value;
                    value = Arrays.asList(ar);
                } else if (value instanceof InputStream) {
                    value = ByteBuffer.wrap(IOUtils.toByteArray(((InputStream) value)));
                }
                map.put(entry.getKey(), value);
            }
        }
        avroShallowResource.setValueMap(map);
        List<AvroShallowResource> children = new LinkedList<AvroShallowResource>();
        if (deep) {
            for (Resource child : resource.getChildren()) {
                String childPath = child.getPath();
                if (!ignoredNodeNames.contains(child.getName())) {
                    children.add(getAvroShallowResource(true, childPath, child));
                }
            }
        }
        avroShallowResource.setChildren(children);
        return avroShallowResource;
    }

    private Collection<AvroShallowResource> readAvroResources(byte[] bytes) throws IOException {
        DatumReader<AvroShallowResource> datumReader = new SpecificDatumReader<AvroShallowResource>(AvroShallowResource.class);
        DataFileReader<AvroShallowResource> dataFileReader = new DataFileReader<AvroShallowResource>(new SeekableByteArrayInput(bytes), datumReader);
        AvroShallowResource avroResource = null;
        Collection<AvroShallowResource> avroResources = new LinkedList<AvroShallowResource>();
        while (dataFileReader.hasNext()) {
// Reuse avroResource object by passing it to next(). This saves us from
// allocating and garbage collecting many objects for files with
// many items.
            avroResource = dataFileReader.next(avroResource);
            avroResources.add(avroResource);
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
        log.info("created resource {}", createdResource);
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
