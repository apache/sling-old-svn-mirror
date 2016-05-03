/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.hapi.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hapi.HApiProperty;
import org.apache.sling.hapi.HApiType;
import org.apache.sling.hapi.HApiUtil;
import org.apache.sling.hapi.MicrodataAttributeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(label = "Apache Sling Hypermedia API tools", metatype = true)
@Service(value = HApiUtil.class)
public class HApiUtilImpl implements HApiUtil {

    private final Logger LOG = LoggerFactory.getLogger(HApiUtil.class);

    @Property(label = "HApi Resource Type", cardinality = 0, value = DEFAULT_RESOURCE_TYPE)
    public static final String HAPI_RESOURCE_TYPE = "org.apache.sling.hapi.tools.resourcetype";

    @Property(label = "HApi Types Search Paths", cardinality=50, value = {"/libs/sling/hapi/types"})
    public static final String HAPI_PATHS = "org.apache.sling.hapi.tools.searchpaths";


    private static final String DEFAULT_SERVER_URL = "http://localhost:8080";
    @Property(label = "External server URL", cardinality = 0, value = DEFAULT_SERVER_URL)
    public static final String HAPI_EXTERNAL_URL = "org.apache.sling.hapi.tools.externalurl";

    public static String resourceType;
    public static String[] hApiPaths;
    public static String serverContextPath;


    @Activate
    private void activate(Map<String, Object> configuration) {
        resourceType = PropertiesUtil.toString(configuration.get(HAPI_RESOURCE_TYPE), DEFAULT_RESOURCE_TYPE);
        hApiPaths = PropertiesUtil.toStringArray(configuration.get(HAPI_PATHS));
        serverContextPath = PropertiesUtil.toString(configuration.get(HAPI_EXTERNAL_URL), DEFAULT_SERVER_URL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public Node getTypeNode(ResourceResolver resolver, String type) throws RepositoryException {
        return getTypeResource(resolver, type).adaptTo(Node.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource getTypeResource(ResourceResolver resolver, String type) throws RepositoryException {
        Session session = resolver.adaptTo(Session.class);

        // Try to resolve the resource as a path
        Resource res = resolver.getResource(type);
        if (null != res) {
            LOG.debug("res = " + res.getName() + " " + res.getPath());
            return res;
        } else {
            for (String path : new HashSet<String>(Arrays.asList(hApiPaths))) {
                // Remove trailing slash from path
                path = (path.endsWith("/")) ? path.substring(0,path.length() - 1) : path;

                // Get the query manager for the session
                QueryManager queryManager = session.getWorkspace().getQueryManager();

                // Build query for the search paths
                StringBuilder queryString = new StringBuilder("SELECT * FROM [nt:unstructured] WHERE ");
                queryString.append(String.format("ISDESCENDANTNODE([%s]) ", path));
                queryString.append(String.format("AND [sling:resourceType]='%s' AND fqdn = '%s'", resourceType, type));

                // Execute query
                Query query = queryManager.createQuery(queryString.toString(), Query.JCR_SQL2);
                LOG.debug("Querying HAPi: {}", queryString.toString());
                QueryResult result = query.execute();

                NodeIterator nodeIter = result.getNodes();
                if (nodeIter.hasNext()) {
                    return resolver.getResource(nodeIter.nextNode().getPath());
                } else {
                    // continue
                }
            }

            // Type has to be abstract
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public HApiType fromPath(ResourceResolver resolver, String type) throws RepositoryException {
        Resource typeResource = this.getTypeResource(resolver, type);
        LOG.debug("typeResource=" + typeResource);
        if (null == typeResource) {
            return new AbstractHapiTypeImpl(type);
        } else {
            return fromResource(resolver, typeResource);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public HApiType fromNode(ResourceResolver resolver, Node typeNode) throws RepositoryException {
        if (null == typeNode) return null;
        Resource resource = resolver.getResource(typeNode.getPath());
        return fromResource(resolver, resource);
    }

    /**
     * {@inheritDoc}
     */
    public HApiType fromResource(ResourceResolver resolver, Resource typeResource) throws RepositoryException {
        if (null == typeResource) return null;

        ValueMap resProps = typeResource.adaptTo(ValueMap.class);
        String name = resProps.get("name", (String) null);
        String description = resProps.get("description", (String) null);
        String path = typeResource.getPath();
        String fqdn = resProps.get("fqdn", (String) null);
        // get parameters
        Value[] parameterValues = resProps.get("parameters", new Value[]{});
        List<String> parameters = new ArrayList<String>(parameterValues.length);

        for (Value p : Arrays.asList(parameterValues)) {
            parameters.add(p.getString());
        }
        HApiTypeImpl newType = new HApiTypeImpl(name, description, serverContextPath, path, fqdn, parameters, null, null, false);
        TypesCache.getInstance(this).addType(newType);


        try {
            // Get parent if it exists
            HApiType parent = null;
            String parentPath = resProps.get("extends", (String) null);
            if (null != parentPath) {
                parent = TypesCache.getInstance(this).getType(resolver, parentPath);
            }

            // Get properties
            Map<String, HApiProperty> properties = new HashMap<String, HApiProperty>();
            for (Resource res : typeResource.getChildren()) {
                ValueMap resValueMap = res.adaptTo(ValueMap.class);

                String propName = res.getName();
                String propDescription = resValueMap.get("description", "");
                String typePath = resValueMap.get("type", (String) null);
                HApiType propType = TypesCache.getInstance(this).getType(resolver, typePath);
                Boolean propMultiple = resValueMap.get("multiple", false);

                HApiProperty prop = new HApiPropertyImpl(propName, propDescription, propType, propMultiple);
                properties.put(prop.getName(), prop);
            }
            // Set parent and properties
            newType.setParent(parent);
            newType.setProperties(properties);

        } catch (RuntimeException t) {
            // Remove type from cache if it wasn't created successfully
            TypesCache.getInstance(this).removeType(newType.getPath());
            throw t;
        } catch (RepositoryException e) {
            // Remove type from cache if it wasn't created successfully
            TypesCache.getInstance(this).removeType(newType.getPath());
            throw e;
        }

        LOG.debug("Created type {}", newType);
        return newType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MicrodataAttributeHelper getHelper(ResourceResolver resolver, String type) throws RepositoryException {
        return new MicrodataAttributeHelperImpl(resolver, TypesCache.getInstance(this).getType(resolver, type));
    }
}

/**
 * <p>Cache for types</p>
 *
 */
class TypesCache {
    private static final Logger LOG = LoggerFactory.getLogger(TypesCache.class);
    Map<String, HApiType> types;
    private static TypesCache singleton = null;
    private HApiUtil hApiUtil;

    public static TypesCache getInstance(HApiUtil hApiUtil) {
        if (null == singleton) {
            singleton = new TypesCache(hApiUtil);
        }
        LOG.debug("singleton: {}", singleton);
        return singleton;
    }

    private TypesCache(HApiUtil hApiUtil) {
        this.types = new HashMap<String, HApiType>();
        this.hApiUtil = hApiUtil;
    }

    public HApiType getType(ResourceResolver resolver, String typePath) throws RepositoryException {
        if (types.containsKey(typePath)) {
            return this.types.get(typePath);
        } else {

            HApiType type = hApiUtil.fromPath(resolver, typePath);
            types.put(type.getPath(), type);
            return type;
        }
    }

    public void addType(HApiType type) {
        this.types.put(type.getPath(), type);
    }

    public void removeType(String path) {
        this.types.remove(path);
    }
}

