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

package org.apache.sling.cassandra.resource.provider;

import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;
import org.apache.sling.adapter.annotations.Adaptable;
import org.apache.sling.adapter.annotations.Adapter;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.cassandra.resource.provider.mapper.CassandraMapperException;
import org.apache.sling.cassandra.resource.provider.util.CassandraResourceProviderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Adaptable(adaptableClass=Resource.class, adapters={
    @Adapter({ValueMap.class})
})
public class CassandraResource extends AbstractResource {

    private String resourcePath;
    private String remainingPath;
    private String columnFamilySector;
    private CassandraResourceProvider resourceProvider;
    private ResourceResolver resourceResolver;
    private static Logger LOGGER = LoggerFactory.getLogger(CassandraResource.class);
    private boolean dataLoaded = false;
    private String resourceSuperType = "nt:supCassandra";
    private String resourceType = "nt:casandra";
    private String metadata = "resolutionPathInfo=json";
    private final ValueMap valueMap;
    private boolean isTransient = false;

    static class CassandraValueMap extends ValueMapDecorator {
        CassandraValueMap(String path) {
            super(new HashMap<String, Object>());
            put("path", path);
        }
    }

    public CassandraResource(ResourceProvider resourceProvider, ResourceResolver resourceResolver, String resourcePath,ValueMap valueMap) {
        this.resourceProvider = (CassandraResourceProvider) resourceProvider;
        this.resourceResolver =  resourceResolver;
        this.resourcePath = resourcePath;
        this.remainingPath = CassandraResourceProviderUtil.getRemainingPath(resourcePath);
        this.columnFamilySector = CassandraResourceProviderUtil.getColumnFamilySector(resourcePath);
        this.valueMap = valueMap;
    }

     public CassandraResource(ResourceProvider resourceProvider, ResourceResolver resourceResolver, String resourcePath,ValueMap valueMap,Map<String, Object> stringObjectMap) {
        this.resourceProvider = (CassandraResourceProvider) resourceProvider;
        this.resourceResolver =  resourceResolver;
        this.resourcePath = resourcePath;
        this.remainingPath = CassandraResourceProviderUtil.getRemainingPath(resourcePath);
        this.columnFamilySector = CassandraResourceProviderUtil.getColumnFamilySector(resourcePath);
        this.valueMap = valueMap;
        this.isTransient = true;
//        this.metadata=stringObjectMap.get("metadata").toString();
//        this.resourceType=stringObjectMap.get("resourceType").toString();
//        this.resourceSuperType=stringObjectMap.get("resourceSuperType").toString();
     }

    private void loadResourceData(ResourceProvider resourceProvider) {
        CassandraResourceProvider cassandraResourceProvider = (CassandraResourceProvider) resourceProvider;
        try {
//          TODO ColumnFamilySector is NULL and hence this..
            String cql = cassandraResourceProvider.getCassandraMapperMap().get(columnFamilySector).getCQL(columnFamilySector, remainingPath);
            QueryResult<CqlRows<String, String, String>> results = CassandraResourceProviderUtil.executeQuery(cql, ((CassandraResourceProvider) resourceProvider).getKeyspace(), new StringSerializer());
            populateDataFromResult(results);
            dataLoaded = true;
        } catch (CassandraMapperException e) {
            System.out.println("Error occurred from resource at " + resourcePath + " : " + e.getMessage());
            LOGGER.error("Error occurred from resource at " + resourcePath + " : " + e.getMessage());
        }
    }

    private void populateDataFromResult(QueryResult<CqlRows<String, String, String>> result) {
        for (Row<String, String, String> row : result.get().getList()) {
            for (HColumn column : row.getColumnSlice().getColumns()) {
                //  Assumed Only one result, since key is unique
                if (column.getName().equals("metadata")) {
                    this.metadata = column.getValue().toString();
                } else if (column.getName().equals("resourceSuperType")) {
                    this.resourceSuperType = column.getValue().toString();
                } else if (column.getName().equals("resourceType")) {
                    this.resourceType = column.getValue().toString();
                }
            }
        }
    }

    public String getPath() {
        return resourcePath;
    }

    @Override
    public String getName() {
        return CassandraResourceProviderUtil.getNameFromPath(resourcePath);
    }

    @Override
    public Resource getParent() {
        return new CassandraResource(this.resourceProvider,
                this.resourceResolver,
                CassandraResourceProviderUtil.getParentPath(resourcePath),valueMap);
    }

    @Override
    public Iterator<Resource> listChildren() {
        List<Resource> children = new ArrayList<Resource>();
        try {
            QueryResult<CqlRows<String, String, String>> result = CassandraResourceProviderUtil.getAllNodes(
                    resourceProvider.getKeyspace(),
                    CassandraResourceProviderUtil.getColumnFamilySector(resourcePath));
            for (Row<String, String, String> row : result.get().getList()) {
                for (HColumn column : row.getColumnSlice().getColumns()) {
                    if ("path".equals(column.getName()) && CassandraResourceProviderUtil.isAnImmediateChild(resourcePath, column.getValue().toString())) {
                        children.add( new CassandraResource(resourceProvider,resourceResolver,column.getValue().toString(),valueMap));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error occurred while getting child nodes " + e.getMessage());
            LOGGER.error("Error occurred while getting child nodes " + e.getMessage());
        }
        return children.iterator();
    }

    @Override
    public Iterable<Resource> getChildren() {
        return new CassandraIterable(listChildren());
    }

    @Override
    public Resource getChild(String s) {
       return resourceProvider.getResource(resourceResolver,new StringBuilder(resourcePath.endsWith("/") ? resourcePath.substring(0, resourcePath.length() - 1)
                        : resourcePath)
                        .append("/").append(s).toString());
//        return new CassandraResource(
//                this.resourceProvider,
//                this.resourceResolver,
//                new StringBuilder(resourcePath.endsWith("/") ? resourcePath.substring(0, resourcePath.length() - 1)
//                        : resourcePath)
//                        .append("/").append(s).toString(),valueMap);
    }

    public String getResourceType() {
        if(!isTransient) {
            loadResourceData(resourceProvider);
        }
        return this.resourceType;
    }

    public String getResourceSuperType() {
        if(!isTransient) {
            loadResourceData(resourceProvider);
        }
        return this.resourceSuperType;
    }

    @Override
    public boolean isResourceType(String s) {
        return super.isResourceType(s);
    }

    public ResourceMetadata getResourceMetadata() {
        if(!isTransient) {
            loadResourceData(resourceProvider);
        }
//       Expected format of metadata is a String; i.e "characterEncoding=UTF-8,resolutionPathInfo=.html"
        if(metadata == null || "".equals(metadata) || metadata.split(",").length == 0) {
            ResourceMetadata resourceMetadata = new ResourceMetadata();
            resourceMetadata.setModificationTime(System.currentTimeMillis());
            resourceMetadata.setResolutionPath(resourcePath);
            return resourceMetadata;
        }
        ResourceMetadata resourceMetadata = new ResourceMetadata();

        for(String ele:metadata.split(",")) {
            String key=ele.split("=")[0].trim();
            String value=ele.split("=")[1].trim();

          if("characterEncoding".equalsIgnoreCase(key)) {
             resourceMetadata.setCharacterEncoding(value);
          } else if("contentType".equalsIgnoreCase(key)) {
              resourceMetadata.setContentType(value);
          } else if("contentLength".equalsIgnoreCase(key)) {
              resourceMetadata.setContentLength(Integer.valueOf(value));
          } else if("resolutionPathInfo".equalsIgnoreCase(key)) {
              resourceMetadata.setResolutionPathInfo(value);
          }
        }

        resourceMetadata.setModificationTime(System.currentTimeMillis());
        resourceMetadata.setResolutionPath(resourcePath);
        return resourceMetadata;
    }

    public ResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }

    @Override
    @SuppressWarnings("unchecked")
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if(type == ValueMap.class) {
            return (AdapterType)valueMap;
        } else if (type == ModifiableValueMap.class) {
            return (AdapterType)valueMap;
        }
        return super.adaptTo(type);
    }
    @Override
    public String toString(){
      return new StringBuilder("{\nresourcePath=").append(resourcePath).
                  append("\n    remainingPath=").append(remainingPath).
                  append("\n    columnFamilySector=").append(columnFamilySector).
                  append("\n    resourceSuperType=").append(resourceSuperType).
                  append("\n    resourceType=").append(resourceType).
                  append("\n    metaData=").append(metadata).append("\n}").toString();
    }

}
