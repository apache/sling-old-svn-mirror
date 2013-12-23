/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.cassandra.resource.provider.security;

import org.apache.commons.codec.binary.Base64;
import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.query.QueryResult;
import org.apache.sling.cassandra.resource.provider.CassandraResourceProvider;
import org.apache.sling.cassandra.resource.provider.util.CassandraResourceProviderUtil;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AccessControlUtil {
    private static Logger LOGGER = LoggerFactory.getLogger(AccessControlUtil.class);

    private static final String ACL_CF = "sling_acl";
    private static final String ACE_SEPARATOR = "_";

   private static final int  READ = 0x01;
   private static final  int  WRITE = 0x02;
   private static final  int  DELETE = 0x04;
    private static final String  _READ = "READ";
    private static final String  _WRITE = "WRITE";
    private static final  String  _DELETE = "DELETE";
    private static final String DEFAULT_SYSTEM_ACE="0_everyone_allow : 0x01";

    private CassandraResourceProvider provider;


    public AccessControlUtil(CassandraResourceProvider provider) {
        this.provider = provider;
    }

    public AccessControlUtil() {
    }

    public static void main(String[] args) throws Exception {

            m();
    }


    private static void m() {

       AccessControlUtil accessControlUtil =  new AccessControlUtil(new CassandraResourceProvider());

        String path = "/content/cassandra/p2/c2";
        String policy="0_dishara_allow : " + READ;
        Set<String> set  = new HashSet<String>();
        set.add("dishara");
        try {

        accessControlUtil.addACE(path,policy);

          int results[] = accessControlUtil.buildAtLevel(set,path);
            System.out.println("GRANT" +results[0]);
            System.out.println("DENY" + results[1]);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getPrivilegeFromACE(String ace) {
     return Integer.decode(ace.split(":")[1].trim());
    }

    private String getPrincipleFromACE(String ace) {
        return ace.split(":")[0].trim().split(ACE_SEPARATOR)[1].trim();
     }

    private boolean isACEGrant(String ace) {
        return "allow".equalsIgnoreCase(ace.split(":")[0].trim().split(ACE_SEPARATOR)[2].trim());
    }

    private boolean isUserHasPrinciple(String principle,Set<String> principles){
        return principles.contains(principle);
    }

    private int[] getCurrentLevelBitmaps(Set<String> userPrinciples, String currentPath) throws Exception {
        int grants = 0;
        int denies = 0;

        String[] aclArray = getACL(currentPath);
        if(aclArray == null){
           return new int[]{grants,denies};
        }
        for (String ace : aclArray) {
            if (isUserHasPrinciple(getPrincipleFromACE(ace),userPrinciples)){
                int toGrant = 0;
                int toDeny = 0;
                if (isACEGrant(ace)) {
                    toGrant = getPrivilegeFromACE(ace);
                } else {
                    toDeny = getPrivilegeFromACE(ace);
                }
                toGrant = toGrant & ~denies;
                toDeny = toDeny & ~grants;
                grants = grants | toGrant;
                denies = denies | toDeny;
            }
        }
        return new int[]{grants, denies};
    }

    private int[] buildAtLevel(Set<String> userPrinciples, String path) throws Exception {
        int[] parentPermission = new int[]{0,0};
        int[] permissions = new int[]{0,0};
        if (!"/".equals(path)) {
            parentPermission = buildAtLevel(userPrinciples, CassandraResourceProviderUtil.getParentPath(path));
        }
        permissions = getCurrentLevelBitmaps(userPrinciples, path);
        int toGrant = parentPermission[0] & ~permissions[1];
        int toDeny = parentPermission[1] & ~permissions[0];
        permissions[0] = permissions[0] | toGrant;
        permissions[1] = permissions[1] | toDeny;
        return permissions;
    }


    private void addACE(String path, String policy) throws Exception {
        String rid = getrowID(path);
        createColumnFamily(ACL_CF, provider.getKeyspace(), new StringSerializer());
        String getAllACEs = "select * from " + ACL_CF + " where KEY = '" + rid + "'";
        QueryResult<CqlRows<String, String, String>> results = CassandraResourceProviderUtil.executeQuery(getAllACEs, provider.getKeyspace(), new StringSerializer());
        if (CassandraResourceProviderUtil.recordExists(results, "policy")) {
            updateACL(rid, policy, new StringSerializer(), results);
        } else {
            addACL(rid, policy, new StringSerializer());
        }
    }


    private String[] getACL(String path) throws Exception {
        if(getCassandraSystemNodeACL(path) != null){
         return getCassandraSystemNodeACL(path);
        }
        String rid = getrowID(path);
        createColumnFamily(ACL_CF, provider.getKeyspace(), new StringSerializer());
        String getAllACEs = "select * from " + ACL_CF + " where KEY = '" + rid + "'";
        QueryResult<CqlRows<String, String, String>> results = CassandraResourceProviderUtil.executeQuery(getAllACEs, provider.getKeyspace(), new StringSerializer());
        String policy = null;
        for (Row<String, String, String> row : ((CqlRows<String, String, String>) results.get()).getList()) {
            for (HColumn column : row.getColumnSlice().getColumns()) {
                if ("policy".equalsIgnoreCase(column.getName().toString()) && column.getValue() != null) {
                    policy = column.getValue().toString();
                }
            }
        }
        return policy != null ? policy.split(";") : null;
    }

    private String[] getCassandraSystemNodeACL(String path){
        if("/content".equals(path) || "/content/cassandra".equals(path) || "/".equals(path))  {
         return new String[]{DEFAULT_SYSTEM_ACE};
        } else {
            return null;
        }
    }



    private void updateACL(String rid, String policy, StringSerializer se, QueryResult<CqlRows<String, String, String>> results) {
        String oldACL = "";
        for (Row<String, String, String> row : ((CqlRows<String, String, String>) results.get()).getList()) {
            for (HColumn column : row.getColumnSlice().getColumns()) {
                if ("policy".equalsIgnoreCase(column.getName().toString()) && column.getValue() != null) {
                    oldACL = column.getValue().toString();
                }
            }
        }

        if (!oldACL.isEmpty()) {
            oldACL = oldACL + ";" + policy;
        }

        addACL(rid, oldACL, new StringSerializer());

    }

    private void addACL(String rid, String policy, StringSerializer se) {
        CqlQuery<String, String, String> cqlQuery = new CqlQuery<String, String, String>(provider.getKeyspace(), se, se, se);
        String data = "'" + rid + "'" + "," + "'" + policy + "'";
        String query = "insert into " + ACL_CF + " (KEY,policy) values (" + data + ");";
        cqlQuery.setQuery(query);
        QueryResult<CqlRows<String, String, String>> result = cqlQuery.execute();
        System.out.println("#####Added ACL");
    }

    private String getrowID(String path) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        String rowID = new String(Base64.encodeBase64(md.digest(CassandraResourceProviderUtil.getRemainingPath(path) != null?CassandraResourceProviderUtil.getRemainingPath(path).getBytes("UTF-8"):"TEST".getBytes())));
        return rowID;
    }

    private void createColumnFamily(String cf, Keyspace keyspace, StringSerializer se) {
        String createCF = "CREATE COLUMNFAMILY " + cf + " (KEY varchar PRIMARY KEY,policy varchar);";
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
