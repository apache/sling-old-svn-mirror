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

package org.apache.sling.cassandra.resource.provider.util;

import org.apache.commons.codec.binary.Base64;
import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.cassandra.resource.provider.CassandraResourceProvider;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraResourceProviderUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(CassandraResourceProviderUtil.class);

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Reference
    private ResourceResolver resourceResolver;


    public static QueryResult<CqlRows<String, String, String>> executeQuery(String query, Keyspace keyspace, StringSerializer se) {
//        path, resourceType,resourceSuperType,metadata
        CqlQuery<String, String, String> cqlQuery = new CqlQuery<String, String, String>(keyspace, se, se, se);
        cqlQuery.setQuery(query);
        return cqlQuery.execute();
    }


    public static String getRemainingPath(String path) {
        if (path.startsWith("/") && path.split("/").length > 4) {
            return path.substring(new StringBuilder("/").
                    append(path.split("/")[1]).
                    append("/").
                    append(path.split("/")[2]).
                    append("/").
                    append(path.split("/")[3]).
                    toString().length() + 1,
                    path.length());
        } else {
            return null;
        }
    }

    public static String getColumnFamilySector(String path) {
        if (path.startsWith("/") && path.split("/").length > 4) {
            return path.split("/")[3];
        } else {
            return null;
        }
    }

    public static void logResults(QueryResult<CqlRows<String, String, String>> result) {
        LOGGER.info("############# RESULT ################");
        for (Row<String, String, String> row : ((CqlRows<String, String, String>) result.get()).getList()) {
            for (HColumn column : row.getColumnSlice().getColumns()) {
                LOGGER.info(column.getValue().toString());
            }
            LOGGER.info("------------------------------------------------------------------------------------");
        }
        LOGGER.info("############# RESULT ################");
    }

    public static boolean recordExists(QueryResult<CqlRows<String, String, String>> result,String key) {
        boolean exists = false;
        for (Row<String, String, String> row : (result.get()).getList()) {
            for (HColumn column : row.getColumnSlice().getColumns()) {
             if(column.getName().equals(key) && column.getValue() != null) {
                 exists = true;
                 break;
             }
            }
        }
        return exists;
    }

    public static boolean isResourceExists(CassandraResourceProvider resourceProvider, Keyspace keyspace, String path) throws Exception {
        String cql = resourceProvider.getCassandraMapperMap().get(CassandraResourceProviderUtil.getColumnFamilySector(path)).getCQL(CassandraResourceProviderUtil.getColumnFamilySector(path), CassandraResourceProviderUtil.getRemainingPath(path));
        QueryResult<CqlRows<String, String, String>> result = CassandraResourceProviderUtil.executeQuery(cql, keyspace, new StringSerializer());
        return recordExists(result,"policy");
    }

    public static String getrowID(String path) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        String rowID = new String(Base64.encodeBase64(md.digest(getRemainingPath(path).getBytes("UTF-8"))));
        return rowID;
    }

    public static String getParentPath(String path) {
        if(path.lastIndexOf("/") == 0){
         return "/";
        }
        return path.substring(0,path.lastIndexOf("/"));
    }

    public static String getNameFromPath(String path){
    return path.substring(path.lastIndexOf("/")+1);
    }


    public static QueryResult<CqlRows<String, String, String>> getAllNodes(Keyspace keyspace, String cf) throws Exception {
        String cql = "select * from " + cf;
        return CassandraResourceProviderUtil.executeQuery(cql, keyspace, new StringSerializer());
    }


    public static boolean isAnImmediateChild(String path,String s){
         if(s.length() > path.length()) {
            return !s.substring(path.length() + 1).contains("/");
         }
        return false;
    }

  //    row.getColumnSlice().getColumnByName("metadata").getValue()

}
