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

import org.apache.commons.codec.binary.Base64;
import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.QueryResult;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.*;
import org.apache.sling.cassandra.resource.provider.mapper.CassandraMapper;
import org.apache.sling.cassandra.resource.provider.mapper.DefaultCassandraMapperImpl;
import org.apache.sling.cassandra.resource.provider.util.CassandraResourceProviderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.Iterator;


@Component(immediate = true, metatype = true)
@Service(ResourceProvider.class)
@Properties({
        @Property(name = "provider.roots", value = {"/content/cassandra"})
})
public class CassandraResourceProvider implements ResourceProvider, ModifyingResourceProvider {

    private Cluster cluster;
    private Keyspace keyspace;
    private String cf = "pictures";
    private int replicationFactor = 1;
    private final static String KEYSPACE = "sling_cassandra";
    private String CASSANDRA_EP = "localhost:9160";
    private ConcurrentHashMap<String, CassandraMapper> cassandraMapperMap
            = new ConcurrentHashMap<String, CassandraMapper>();
    private static Logger LOGGER = LoggerFactory.getLogger(CassandraResourceProvider.class);
    private static final Map<String, ValueMap> CASSANDRA_MAP = new HashMap<String, ValueMap>();
    private static final Map<String, Map> TRANSIENT_CREATE_MAP = new HashMap<String, Map>();
    private static final Map<String, Map> TRANSIENT_DELETE_MAP = new HashMap<String, Map>();


    public CassandraResourceProvider() {
        init();
    }

    public Keyspace getKeyspace() {
        return keyspace;
    }

    public void setColumnFamily(String cf) {
        this.cf = cf;
    }

    private synchronized void init() {
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ PROVIDER INIT");
        cluster = HFactory.getOrCreateCluster(CassandraConstants.CASSANDRA_DEFAULT_CLUSTER_NAME, CASSANDRA_EP);
        KeyspaceDefinition keyspaceDef = cluster.describeKeyspace(KEYSPACE);
        if (keyspaceDef == null) {
            try {
                createSchema(cluster);
            } catch (Exception ignore) {
                System.out.println("Ignoring the exception when trying to create a already existing schema " + ignore.getMessage());
                LOGGER.debug("Ignoring the exception when trying to create a already existing schema " + ignore.getMessage());
            }
        }
        this.keyspace = HFactory.createKeyspace(KEYSPACE, cluster);
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ PROVIDER INIT2");

    }

    public Resource getResource(ResourceResolver resourceResolver, javax.servlet.http.HttpServletRequest httpServletRequest, String s) {
        return getResource(resourceResolver, s);
    }

    public Resource getResource(ResourceResolver resourceResolver, String s) {
        //Populating the map of cassandra mappers.
        if(s.startsWith("/") && !s.equals("/content/cassandra") && s.split("/").length <= 4){
            return new CassandraResource(this, resourceResolver, s, new CassandraResource.CassandraValueMap(s),new HashMap<String, Object>());
        }
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"+s);

        if (CASSANDRA_MAP.get(s) == null) {
            return null;
        }

        if (CassandraResourceProviderUtil.getColumnFamilySector(s) != null && !getCassandraMapperMap().containsKey(CassandraResourceProviderUtil.getColumnFamilySector(s))) {
            getCassandraMapperMap().put(CassandraResourceProviderUtil.getColumnFamilySector(s), new DefaultCassandraMapperImpl());
        }
        try {
            return new CassandraResource(this, resourceResolver, s, CASSANDRA_MAP.get(s));
//            return CassandraResourceProviderUtil.isResourceExists(this,keyspace,s) ? new CassandraResource(this,resourceResolver, s,CASSANDRA_MAP.get(s)) : null;
        } catch (Exception e) {
            System.out.println("Error at Provider " + e.getMessage());
            LOGGER.error("Error at Provider " + e.getMessage());
            LOGGER.debug(e.getMessage());
        }
        return null;
    }

    public Iterator<Resource> listChildren(Resource resource) {
        return resource.listChildren();
    }

    private void createSchema(Cluster myCluster) {
        ColumnFamilyDefinition cfDef = HFactory.createColumnFamilyDefinition(KEYSPACE, cf, ComparatorType.BYTESTYPE);
        KeyspaceDefinition newKeyspace = HFactory.createKeyspaceDefinition(
                KEYSPACE,
                ThriftKsDef.DEF_STRATEGY_CLASS,
                replicationFactor,
                Arrays.asList(cfDef));
        myCluster.addKeyspace(newKeyspace, true);
    }

    public ConcurrentHashMap<String, CassandraMapper> getCassandraMapperMap() {
        return cassandraMapperMap;
    }

    static {
        defineCassandra("/content/cassandra/p1/c1");
        defineCassandra("/content/cassandra/p1/c1/c2");
        defineCassandra("/content/cassandra/nK/1");
        loadMapWithTestData();
    }

    private static void loadMapWithTestData() {
        String[] nodes = new String[]{"A", "B", "C", "D", "E", "F"};
        for (String node : nodes) {
            int exerciseCount = 0;
            int testCount = 0;
            for (int i = 0; i < 1000; i++) {
                String path = "/content/cassandra/" + node + "/" + i;
                if (i % 2 == 0) {
                    if (exerciseCount <= 100) {
                        defineCassandra(path);
                        exerciseCount++;
                    }
                } else {
                    if (testCount <= 100) {
                        defineCassandra(path);
                        testCount++;
                    }
                }
            }

        }
    }


    private static ValueMap defineCassandra(String path) {
        final ValueMap valueMap = new CassandraResource.CassandraValueMap(path);
        CASSANDRA_MAP.put(path, valueMap);
        return valueMap;
    }


    /**
     * Modification methods for Cassandra Resource Provider
     */

    @Override
    public Resource create(ResourceResolver resourceResolver, String s, Map<String, Object> stringObjectMap) throws PersistenceException {
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@AA3 CREATE");
        TRANSIENT_CREATE_MAP.put(s, stringObjectMap);
        commit(resourceResolver);
//        return getResource(resourceResolver,s);
        return new CassandraResource(this, resourceResolver, s, new CassandraResource.CassandraValueMap(s),stringObjectMap);
    }

    @Override
    public void delete(ResourceResolver resourceResolver, String s) throws PersistenceException {
        if (TRANSIENT_CREATE_MAP.get(s) != null) {
            TRANSIENT_DELETE_MAP.put(s, TRANSIENT_CREATE_MAP.get(s));
            TRANSIENT_CREATE_MAP.remove(s);
        }

        if (CASSANDRA_MAP.get(s) != null) {
            TRANSIENT_DELETE_MAP.put(s, CASSANDRA_MAP.get(s));
        }
    }

    @Override
    public void revert(ResourceResolver resourceResolver) {
        TRANSIENT_CREATE_MAP.clear();
        TRANSIENT_DELETE_MAP.clear();
    }

    @Override
    public void commit(ResourceResolver resourceResolver) throws PersistenceException {
        for (Map.Entry<String, Map> entry : TRANSIENT_CREATE_MAP.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                if(insertResource(entry.getKey(), entry.getValue())){
                    defineCassandra(entry.getKey());
                }
            }
        }

        for (Map.Entry<String, Map> entry : TRANSIENT_DELETE_MAP.entrySet()) {
            if(deleteResource(resourceResolver, entry.getKey())){
                CASSANDRA_MAP.remove(entry.getKey());
            }
        }

    }

    private boolean deleteResource(ResourceResolver resourceResolver, String path) throws PersistenceException {
        try {
            String key = getrowID(path);
            if(key == null){
            return false;
            }
            String _cf = CassandraResourceProviderUtil.getColumnFamilySector(path);
            createColumnFamily(_cf, this.getKeyspace(), new StringSerializer());

            StringSerializer se = new StringSerializer();
            CqlQuery<String, String, String> cqlQuery = new CqlQuery<String, String, String>(keyspace, se, se, se);
            String query = "delete FROM " + _cf + " where KEY = '" + key + "';";
            cqlQuery.setQuery(query);
            QueryResult<CqlRows<String, String, String>> result = cqlQuery.execute();
        } catch (NoSuchAlgorithmException e) {
            throw new PersistenceException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new PersistenceException(e.getMessage());
        }
        return true;

    }

    private boolean insertResource(String path, Map map) throws PersistenceException {
        try {
            String r = getrowID(path);
            if(r == null){
             return false;
            }
            String _cf = CassandraResourceProviderUtil.getColumnFamilySector(path);
            createColumnFamily(_cf, this.getKeyspace(), new StringSerializer());
            String metadata = map.get("metadata") == null? "resolutionPathInfo=json":(String)map.get("metadata");
            String resourceType =  map.get("resourceType") == null?"nt:cassandra":(String)map.get("resourceType");
            String resourceSuperType =  map.get("resourceSuperType") == null?"nt:superCassandra":(String) map.get("resourceSuperType");

            addData(this.getKeyspace(), _cf, new StringSerializer(),
                    new String[]{
                            "'" + r + "','" + path + "','" + resourceType + "','" + resourceSuperType + "','" + metadata + "'",
                    });
        } catch (NoSuchAlgorithmException e) {
            throw new PersistenceException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new PersistenceException(e.getMessage());
        }
      return true;
    }

    private void addData(Keyspace keyspace, String cf, StringSerializer se, String[] dataArray) {
        for (String data : dataArray) {
            CqlQuery<String, String, String> cqlQuery = new CqlQuery<String, String, String>(keyspace, se, se, se);
            String query = "insert into " + cf + " (KEY,path,resourceType,resourceSuperType,metadata) values (" + data + ");";
            cqlQuery.setQuery(query);
            QueryResult<CqlRows<String, String, String>> result = cqlQuery.execute();
            LOGGER.info("Added " + result.toString());
        }
    }

    private void createColumnFamily(String cf, Keyspace keyspace, StringSerializer se) {
        String createCF = "CREATE COLUMNFAMILY " + cf + " (KEY varchar PRIMARY KEY,path varchar,resourceType varchar,resourceSuperType varchar,metadata varchar);";
        CqlQuery<String, String, String> cqlQuery = new CqlQuery<String, String, String>(keyspace, se, se, se);
        cqlQuery.setQuery(createCF);
        try{
        QueryResult<CqlRows<String, String, String>> result1 = cqlQuery.execute();
        LOGGER.info(result1.get().getList().size() + " Finished.!");
        }catch(HInvalidRequestException ignore){
            LOGGER.debug("Column Family already exists " + ignore.getMessage());
        }
    }

    private String getrowID(String path) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        String remPath = CassandraResourceProviderUtil.getRemainingPath(path);
        if(remPath == null ) {
        return null;
        } else {
        String rowID = new String(Base64.encodeBase64(md.digest(remPath.getBytes("UTF-8"))));
        return rowID;
        }
    }

    @Override
    public boolean hasChanges(ResourceResolver resourceResolver) {
        if (TRANSIENT_CREATE_MAP.isEmpty() && TRANSIENT_DELETE_MAP.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }
}
