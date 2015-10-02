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
package org.apache.sling.nosql.couchbase.resourceprovider.impl;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.nosql.couchbase.client.CouchbaseClient;
import org.apache.sling.nosql.couchbase.client.CouchbaseKey;
import org.apache.sling.nosql.generic.adapter.AbstractNoSqlAdapter;
import org.apache.sling.nosql.generic.adapter.MultiValueMode;
import org.apache.sling.nosql.generic.adapter.NoSqlData;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.consistency.ScanConsistency;

/**
 * {@link org.apache.sling.nosql.generic.adapter.NoSqlAdapter} implementation for Couchbase.
 */
public final class CouchbaseNoSqlAdapter extends AbstractNoSqlAdapter {

    private static final String PN_PATH = "path";
    private static final String PN_PARENT_PATH = "parentPath";
    private static final String PN_DATA = "data";

    private final CouchbaseClient couchbaseClient;
    private final String cacheKeyPrefix;
    
    private static final N1qlParams N1QL_PARAMS = N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS);

    public CouchbaseNoSqlAdapter(CouchbaseClient couchbaseClient, String cacheKeyPrefix) {
        this.couchbaseClient = couchbaseClient;
        this.cacheKeyPrefix = cacheKeyPrefix;
        
        // make sure primary index and index on parentPath is present - ignore error if it is already present
        Bucket bucket = couchbaseClient.getBucket();
        bucket.query(N1qlQuery.simple("CREATE PRIMARY INDEX ON `" + couchbaseClient.getBucketName() + "`"));
        bucket.query(N1qlQuery.simple("CREATE INDEX " + PN_PARENT_PATH + " ON `" + couchbaseClient.getBucketName() + "`(" + PN_PARENT_PATH + ")"));
    }

    @Override
    public boolean validPath(String path) {
        return (couchbaseClient != null && couchbaseClient.isEnabled());
    }

    @Override
    public NoSqlData get(String path) {
        Bucket bucket = couchbaseClient.getBucket();
        String cacheKey = CouchbaseKey.build(path, cacheKeyPrefix);
        JsonDocument doc = bucket.get(cacheKey);
        if (doc == null) {
            return null;
        }
        else {
            JsonObject data = doc.content().getObject(PN_DATA);
            if (data == null) {
                return null;
            }
            else {
                return new NoSqlData(path, data.toMap(), MultiValueMode.LISTS);
            }
        }
    }

    @Override
    public Iterator<NoSqlData> getChildren(String parentPath) {
        Bucket bucket = couchbaseClient.getBucket();
        // fetch all direct children of this path
        N1qlQuery query = N1qlQuery.simple(select("*")
                .from(couchbaseClient.getBucketName())
                .where(x(PN_PARENT_PATH).eq(s(parentPath))),
                N1QL_PARAMS);
        N1qlQueryResult queryResult = bucket.query(query);
        handleQueryError(queryResult);
        final Iterator<N1qlQueryRow> results = queryResult.iterator();
        return new Iterator<NoSqlData>() {
            @Override
            public boolean hasNext() {
                return results.hasNext();
            }

            @Override
            public NoSqlData next() {
                JsonObject item = results.next().value();
                JsonObject envelope = item.getObject(couchbaseClient.getBucketName());
                String path = envelope.getString(PN_PATH);
                JsonObject data = envelope.getObject(PN_DATA);
                return new NoSqlData(path, data.toMap(), MultiValueMode.LISTS);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean store(NoSqlData data) {
        Bucket bucket = couchbaseClient.getBucket();
        String cacheKey = CouchbaseKey.build(data.getPath(), cacheKeyPrefix);

        JsonObject envelope = JsonObject.create();
        envelope.put(PN_PATH, data.getPath());
        envelope.put(PN_DATA, JsonObject.from(data.getProperties(MultiValueMode.LISTS)));

        // for list-children query efficiency store parent path as well
        String parentPath = ResourceUtil.getParent(data.getPath());
        if (parentPath != null) {
            envelope.put(PN_PARENT_PATH, parentPath);
        }
        
        JsonDocument doc = JsonDocument.create(cacheKey, envelope);
        try {
            bucket.insert(doc);
            return true; // created
        }
        catch (DocumentAlreadyExistsException ex) {
            bucket.upsert(doc);
            return false; // updated
        }
    }

    @Override
    public boolean deleteRecursive(String path) {
        Bucket bucket = couchbaseClient.getBucket();
        // fetch all descendants and self for deletion
        Pattern descendantsAndSelf = Pattern.compile("^" + path + "(/.+)?$");
        N1qlQuery query = N1qlQuery.simple(select("*")
                .from(couchbaseClient.getBucketName())
                .where("REGEXP_LIKE(`" + PN_PATH + "`, '" + descendantsAndSelf.pattern() + "')"),
                N1QL_PARAMS);
        N1qlQueryResult queryResult = bucket.query(query);
        handleQueryError(queryResult);
        final Iterator<N1qlQueryRow> results = queryResult.iterator();
        boolean deletedAny = false;
        while (results.hasNext()) {
            JsonObject item = results.next().value();
            JsonObject envelope = item.getObject(couchbaseClient.getBucketName());
            String itemPath = envelope.getString(PN_PATH);
            String itemCacheKey = CouchbaseKey.build(itemPath, cacheKeyPrefix);
            bucket.remove(itemCacheKey);
            deletedAny = true;
        }
        return deletedAny;
    }
    
    private void handleQueryError(N1qlQueryResult queryResult) {
        if (!queryResult.parseSuccess()) {
            throw new RuntimeException("Couchbase query parsing error: " + StringUtils.join(queryResult.errors(), "\n"));
        }
        if (!queryResult.finalSuccess()) {
            throw new RuntimeException("Couchbase query error: " + StringUtils.join(queryResult.errors(), "\n"));
        }
    }

}
