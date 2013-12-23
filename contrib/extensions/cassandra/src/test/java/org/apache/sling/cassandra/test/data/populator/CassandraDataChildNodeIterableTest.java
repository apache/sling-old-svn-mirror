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

package org.apache.sling.cassandra.test.data.populator;


import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.Assert;
import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.query.QueryResult;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.cassandra.resource.provider.CassandraResourceProvider;
import org.apache.sling.cassandra.resource.provider.CassandraResourceResolver;
import org.apache.sling.cassandra.resource.provider.util.CassandraResourceProviderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public class CassandraDataChildNodeIterableTest {
    private static Logger LOGGER = LoggerFactory.getLogger(CassandraDataChildNodeIterableTest.class);

    @Test
    public void testGetChildrenData() {
        String cf = "p1";
        try {
            String r1 = getrowID("/content/cassandra/" + cf + "/c1");
            String r2 = getrowID("/content/cassandra/" + cf + "/c1/c2");
            CassandraResourceProvider cassandraResourceProvider = new CassandraResourceProvider();
            createColumnFamily(cf, cassandraResourceProvider.getKeyspace(), new StringSerializer());
            cassandraResourceProvider.setColumnFamily(cf);

          addData(cassandraResourceProvider.getKeyspace(), cf, new StringSerializer(),
                    new String[]{
                            "'" + r1 + "','/content/cassandra/" + cf + "/c1','nt:cassandra1','nt:supercass1','resolutionPathInfo=json'",
                            "'" + r2 + "','/content/cassandra/" + cf + "/c1/c2','nt:cassandra','nt:supercass2','resolutionPathInfo=json'"
                    });

            getChildrenData(cassandraResourceProvider, cf);
       } catch (Exception e) {
            LOGGER.info("Ignore err" + e.getMessage());
       }
    }

    private void getChildrenData(CassandraResourceProvider cassandraResourceProvider, String cf) {
        Resource resource = cassandraResourceProvider.getResource(new CassandraResourceResolver(), "/content/cassandra/" + cf + "/c1");
        Assert.assertNotNull(resource);
        Iterable<Resource> iterableChildren = resource.getChildren();
        Iterator<Resource> children = iterableChildren.iterator();
        boolean hasChild = false;
        while(children.hasNext()){
            Resource r = children.next();
            if(r.getPath().equals("/content/cassandra/" + cf + "/c1/c2")) {
            Assert.assertNotNull(r);
            Assert.assertEquals("/content/cassandra/" + cf + "/c1/c2",r.getPath());
            hasChild = true;
            }
        }
        Assert.assertTrue(hasChild);
    }

    private String getrowID(String path) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        String rowID = new String(Base64.encodeBase64(md.digest(CassandraResourceProviderUtil.getRemainingPath(path).getBytes("UTF-8"))));
        return rowID;
    }

    private void createColumnFamily(String cf, Keyspace keyspace, StringSerializer se) {
        String createCF = "CREATE COLUMNFAMILY " + cf + " (KEY varchar PRIMARY KEY,path varchar,resourceType varchar,resourceSuperType varchar,metadata varchar);";
        CqlQuery<String, String, String> cqlQuery = new CqlQuery<String, String, String>(keyspace, se, se, se);
        cqlQuery.setQuery(createCF);
        try{
        QueryResult<CqlRows<String, String, String>> result1 = cqlQuery.execute();
        LOGGER.info(result1.get().getList().size() + " Finished.!");
        }catch(HInvalidRequestException ignore){
            LOGGER.debug("Column Family already exists "+ignore.getMessage());
        }
    }

    private void addData(Keyspace keyspace, String cf, StringSerializer se, String[] dataArray) {
        for (String data : dataArray) {
            CqlQuery<String, String, String> cqlQuery = new CqlQuery<String, String, String>(keyspace, se, se, se);
            String query = "insert into " + cf + " (KEY,path,resourceType,resourceSuperType,metadata) values (" + data + ");";
            cqlQuery.setQuery(query);
            cqlQuery.execute();
        }
    }
}

