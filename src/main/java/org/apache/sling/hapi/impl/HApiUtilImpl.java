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

import org.apache.sling.hapi.HApiProperty;
import org.apache.sling.hapi.HApiType;
import org.apache.sling.hapi.MicrodataAttributeHelper;
import org.apache.sling.hapi.HApiUtil;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Property;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.*;


@Component(label = "Apache Sling Hypermedia API tools", metatype = true)
@Service()

public class HApiUtilImpl implements HApiUtil {

    @Property(label = "HApi Resource Type", cardinality = 0, value = DEFAULT_RESOURCE_TYPE)
    public static final String HAPI_RESOURCE_TYPE = "org.apache.sling.hapi.tools.resourcetype";


    @Property(label = "HApi Types Search Paths", cardinality=50, value = {"/libs/sling/hapi/types"})
    public static final String HAPI_PATHS = "org.apache.sling.hapi.tools.searchpaths";

    public static String resourceType;
    public static String[] hApiPaths;


    @Activate
    private void activate(ComponentContext context, Map<String, Object> configuration) {
        resourceType = PropertiesUtil.toString(configuration.get(HAPI_RESOURCE_TYPE), DEFAULT_RESOURCE_TYPE);
        hApiPaths = PropertiesUtil.toStringArray(configuration.get(HAPI_PATHS));
    }

    /**
     * {@inheritDoc}
     */
    public Node getTypeNode(ResourceResolver resolver, String type) throws RepositoryException {
        Session session = resolver.adaptTo(Session.class);

        // Try to resolve the resource as a path
        Resource res = resolver.getResource(type);
        if (null != res) {
            LOG.debug("res = " + res.getName() + " " + res.getPath());
            return res.adaptTo(Node.class);
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
                    return nodeIter.nextNode();
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
    public HApiType fromPath(ResourceResolver resolver, String type) throws RepositoryException {
        Node typeNode = this.getTypeNode(resolver, type);
        LOG.debug("typeNode=" + typeNode);
        if (null == typeNode) {
            return new AbstractHapiTypeImpl(type);
        } else {
            return fromNode(resolver, typeNode);
        }
    }

    /**
     * {@inheritDoc}
     */
    public HApiType fromNode(ResourceResolver resolver, Node typeNode) throws RepositoryException {
        if (null == typeNode) return null;
        String name = typeNode.getProperty("name").getValue().getString();
        String description = typeNode.getProperty("description").getValue().getString();
        String path = typeNode.getPath();
        String fqdn = typeNode.getProperty("fqdn").getValue().getString();

        // get parent if it exists
        HApiType parent = null;
        String parentPath = typeNode.hasProperty("extends") ? typeNode.getProperty("extends").getString() : null;
        if (null != parentPath) {
            parent = this.fromPath(resolver, parentPath);
        }

        // get parameters
        Value[] parameterValues = typeNode.hasProperty("parameters") ? typeNode.getProperty("parameters").getValues() : new Value[]{};
        List<String> parameters = new ArrayList<String>(parameterValues.length);

        for (Value p : Arrays.asList(parameterValues)) {
            parameters.add(p.getString());
        }

        // Get properties
        Map<String, HApiProperty> properties = new HashMap<String, HApiProperty>();

        // Add the properties from this node
        Iterator<Node> it = typeNode.getNodes();
        while (it.hasNext()) {
            Node propNode = it.next();
            String propName  = propNode.getName();
            String propDescription = propNode.hasProperty("description") ? propNode.getProperty("description").getString() : "";

            // TODO: maybe create adapter and use adaptTo()
            // TODO: this could be slow, the types can be instantiated externally in a service activate()
            String type = propNode.getProperty("type").getValue().getString();
            HApiType propType = this.fromPath(resolver, type);
            Boolean propMultiple = propNode.hasProperty("multiple") ? propNode.getProperty("multiple").getBoolean() : false;

            HApiProperty prop = new HApiPropertyImpl(propName, propDescription, propType, propMultiple);
            properties.put(prop.getName(), prop);
        }
        return new HApiTypeImpl(name, description, path, fqdn, parameters, properties, parent, false);
    }

    /**
     * {@inheritDoc}
     */
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
}

