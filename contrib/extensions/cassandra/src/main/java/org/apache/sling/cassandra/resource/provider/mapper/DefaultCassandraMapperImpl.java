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

package org.apache.sling.cassandra.resource.provider.mapper;


import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *  This class generates a unique key for a given path and that key is used as the primary key when the particular resource stored in Cassandra.
 *  Key generates by hashing the UTF-8 bytes of the path string with SHA1 function.
 */
public class DefaultCassandraMapperImpl implements CassandraMapper {

    public String getCQL(String columnFamilySelector, String path) throws CassandraMapperException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
            String rowID = new String(Base64.encodeBase64(md.digest(path.getBytes("UTF-8"))));
            return "select * from "+columnFamilySelector+" where KEY = '"+rowID+"'";
        } catch (NoSuchAlgorithmException e) {
            throw  new CassandraMapperException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw  new CassandraMapperException(e.getMessage());
        }
    }
}
