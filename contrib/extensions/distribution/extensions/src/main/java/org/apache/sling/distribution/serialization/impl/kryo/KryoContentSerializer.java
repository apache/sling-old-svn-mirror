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
package org.apache.sling.distribution.serialization.impl.kryo;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kryo based {@link DistributionContentSerializer}
 */
public class KryoContentSerializer implements DistributionContentSerializer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;
    private final Set<String> ignoredProperties;
    private final Set<String> ignoredNodeNames;

    public KryoContentSerializer(String name) {
        this.name = name;

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
    }

    @Override
    public void exportToStream(ResourceResolver resourceResolver, DistributionRequest request, OutputStream outputStream) throws DistributionException {

        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        kryo.addDefaultSerializer(Resource.class, new ResourceSerializer());
        kryo.addDefaultSerializer(InputStream.class, new InputStreamSerializer());
        String[] paths = request.getPaths();
        Output output = new Output(outputStream);
        LinkedList<Resource> resources = new LinkedList<Resource>();
        for (String p : paths) {
            Resource resource = resourceResolver.getResource(p);
            if (resource != null) {
                addResource(request, resources, resource);
            }
        }
        kryo.writeObject(output, resources);
        output.flush();

    }

    @Override
    public void importFromStream(ResourceResolver resourceResolver, InputStream stream) throws DistributionException {
        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        kryo.addDefaultSerializer(Resource.class, new ResourceSerializer());
        kryo.addDefaultSerializer(InputStream.class, new InputStreamSerializer());
        try {
            Input input = new Input(stream);
            LinkedList<Resource> resources = (LinkedList<Resource>) kryo.readObject(input, LinkedList.class);
            input.close();
            for (Resource resource : resources) {
                persistResource(resourceResolver, resource);
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

    private void persistResource(@Nonnull ResourceResolver resourceResolver, Resource resource) throws PersistenceException {
        String path = resource.getPath().trim();
        String name = path.substring(path.lastIndexOf('/') + 1);
        String substring = path.substring(0, path.lastIndexOf('/'));
        String parentPath = substring.length() == 0 ? "/" : substring;
        Resource existingResource = resourceResolver.getResource(path);
        if (existingResource != null) {
            resourceResolver.delete(existingResource);
        }
        Resource parent = resourceResolver.getResource(parentPath);
        if (parent == null) {
            parent = createParent(resourceResolver, parentPath);
        }
        Resource createdResource = resourceResolver.create(parent, name, resource.getValueMap());
        log.debug("created resource {}", createdResource);
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

    private class ResourceSerializer extends Serializer<Resource> {

        @Override
        public void write(Kryo kryo, Output output, Resource resource) {
            ValueMap valueMap = resource.getValueMap();

            output.writeString(resource.getPath());
            output.writeString(resource.getResourceType());

            HashMap map = new HashMap<String, Object>();
            for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                if (!ignoredProperties.contains(entry.getKey())) {
                    map.put(entry.getKey(), entry.getValue());
                }
            }

            kryo.writeObjectOrNull(output, map, HashMap.class);
        }

        @Override
        public Resource read(Kryo kryo, Input input, Class<Resource> type) {

            String path = input.readString();
            String resourceType = input.readString();

            final HashMap<String, Object> map = kryo.readObjectOrNull(input, HashMap.class);

            return new SyntheticResource(null, path, resourceType) {
                @Override
                public ValueMap getValueMap() {
                    return new ValueMapDecorator(map);
                }
            };
        }

    }

    private class ValueMapSerializer extends Serializer<ValueMap> {
        @Override
        public void write(Kryo kryo, Output output, ValueMap valueMap) {
            for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                output.writeString(entry.getKey());
                output.writeString(entry.getValue().toString());
            }
        }

        @Override
        public ValueMap read(Kryo kryo, Input input, Class<ValueMap> type) {
            final Map<String, Object> map = new HashMap<String, Object>();

            String key;
            while ((key = input.readString()) != null) {
                String value = input.readString();
                map.put(key, value);
            }
            return new ValueMapDecorator(map);
        }
    }

    private class InputStreamSerializer extends Serializer<InputStream> {
        @Override
        public void write(Kryo kryo, Output output, InputStream stream) {
            try {
                byte[] bytes = IOUtils.toByteArray(stream);
                output.writeInt(bytes.length);
                output.write(bytes);
            } catch (IOException e) {
                log.warn("could not serialize input stream", e);
            }
        }

        @Override
        public InputStream read(Kryo kryo, Input input, Class<InputStream> type) {
            int size = input.readInt();
            byte[] bytes = new byte[size];
            input.readBytes(bytes);
            return new ByteArrayInputStream(bytes);
        }
    }

    private void addResource(DistributionRequest request, LinkedList<Resource> resources, Resource resource) {
        resources.add(resource);
        String path = resource.getPath();
        boolean deep = request.isDeep(path);
        String[] filters = request.getFilters(path);
        if (deep) {
            for (Resource child : resource.getChildren()) {
                if (!ignoredNodeNames.contains(resource.getName())) {
                    addResource(request, resources, child);
                }
            }
        } else {
            for (Resource child : resource.getChildren()) {
                String childPath = child.getPath();
                if (filtersAllow(filters, childPath) && !ignoredNodeNames.contains(child.getName())) {
                    addResource(request, resources, child);
                }
            }
        }

    }

    private boolean filtersAllow(String[] filters, String path) {
        boolean allowed = false;
        for (String pattern : filters) {
            if (pattern.startsWith("+")) {
                if (Pattern.compile(pattern.substring(1)).matcher(path).matches()) {
                    allowed = true;
                }
            } else if (pattern.startsWith("-")) {
                if (Pattern.compile(pattern.substring(1)).matcher(path).matches()) {
                    allowed = false;
                }
            } else {
                allowed = Pattern.compile(pattern).matcher(path).matches();
            }
        }
        return allowed;
    }


}
