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

public class CassandraModifyResourceProviderAddTest {
    private static Logger LOGGER = LoggerFactory.getLogger(CassandraModifyResourceProviderAddTest.class);

    @Test
    public void testAddData() {
        String cf = "p2";
        try {
            String path1 ="/content/cassandra/" + cf + "/c1";

            CassandraResourceProvider cassandraResourceProvider = new CassandraResourceProvider();
            createColumnFamily(cf, cassandraResourceProvider.getKeyspace(), new StringSerializer());
            cassandraResourceProvider.setColumnFamily(cf);

            Map<String,Object> map1 = new HashMap<String, Object>();
            map1.put("metadata", "resolutionPathInfo=json");
            map1.put("resourceType", "nt:cassandra0");
            map1.put("resourceSuperType", "nt:supercass1");

            CassandraResourceResolver resolver =  new CassandraResourceResolver();
            cassandraResourceProvider.create(resolver,path1,map1);
            Assert.assertNull("Before Commiting Resource should be null",cassandraResourceProvider.getResource(resolver,path1));
            cassandraResourceProvider.commit(resolver);
            Assert.assertNotNull("Commited Resource cannot be null",cassandraResourceProvider.getResource(resolver,path1));

        } catch (Exception e) {
            LOGGER.info("Ignore err" + e.getMessage());
            Assert.fail("Failed to add data to cassandra");
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

}

