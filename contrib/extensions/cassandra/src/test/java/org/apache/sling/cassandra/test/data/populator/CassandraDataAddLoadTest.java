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
import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.query.QueryResult;
import org.apache.sling.cassandra.resource.provider.CassandraResource;
import org.apache.sling.cassandra.resource.provider.CassandraResourceProvider;
import org.apache.sling.cassandra.resource.provider.CassandraResourceResolver;
import org.apache.sling.cassandra.resource.provider.util.CassandraResourceProviderUtil;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Can build a test profile that adds
 * 1K,10K,100K,1M items to Cassandra, each under 1 parent collection.
 * eg
 * content/cassandra/1K/0   to /content/cassandra/1K/999
 * content/cassandra/10K/0   to /content/cassandra/10K/9999
 * content/cassandra/100K/0   to /content/cassandra/100K/99999
 * content/cassandra/1M/0   to /content/cassandra/1M/999999
 * <p/>
 * To add 1000 nodes as content/cassandra/1K/0 to /content/cassandra/1K/999
 * set parentPath = "/content/cassandra/";
 * set count = 1000
 * set cf="1K"
 * And run the test.
 */

public class CassandraDataAddLoadTest {
    private static Logger LOGGER = LoggerFactory.getLogger(CassandraDataAddLoadTest.class);
    private int count = 10;
    private String[] cfs = new String[]{"A1", "B1", "C1", "D1"};
    private int[] sizes = new int[]{10, 100, 1000, 10000};

    private String parentPath = "/content/cassandra/";

    public static void main(String[] args) {

        String path="/content/cassandra/foo";
       if(path.startsWith("/") && path.split("/").length > 4){
           System.out.println(path.split("/").length);
       } else {
           System.out.println(">> "+path.split("/").length);
       }
//        new CassandraDataAddLoadTest().testAddLoadTestData();
    }


    public void testAddLoadTestData() {
        try {

            for (int k = 0; k < sizes.length; k++) {
                CassandraResourceProvider cassandraResourceProvider = new CassandraResourceProvider();
                createColumnFamily(cfs[k], cassandraResourceProvider.getKeyspace(), new StringSerializer());
                cassandraResourceProvider.setColumnFamily(cfs[k]);
                CassandraResourceResolver resolver = new CassandraResourceResolver();
                for (int i = 0; i < sizes[k]; i++) {
                    String path = parentPath + cfs[k] + "/" + i;
                    Map<String, Object> map1 = new HashMap<String, Object>();
                    map1.put("metadata", "resolutionPathInfo=json");
                    map1.put("resourceType", "nt:cassandra0");
                    map1.put("resourceSuperType", "nt:supercass1");
                    cassandraResourceProvider.create(resolver, path, map1);
                    cassandraResourceProvider.commit(resolver);
                    System.out.println(">>" + path);
                }
            }
        } catch (Exception e) {
            LOGGER.info("Ignore err" + e.getMessage());
            Assert.fail("Failed to add data to cassandra");
        }

    }

    private void createColumnFamily(String cf, Keyspace keyspace, StringSerializer se) {
        String createCF = "CREATE COLUMNFAMILY " + cf + " (KEY varchar PRIMARY KEY,path varchar,resourceType varchar,resourceSuperType varchar,metadata varchar);";
        CqlQuery<String, String, String> cqlQuery = new CqlQuery<String, String, String>(keyspace, se, se, se);
        cqlQuery.setQuery(createCF);
        try {
            QueryResult<CqlRows<String, String, String>> result1 = cqlQuery.execute();
            LOGGER.info(result1.get().getList().size() + " Finished.!");
        } catch (HInvalidRequestException ignore) {
            LOGGER.debug("Column Family already exists " + ignore.getMessage());
        }
    }

}

