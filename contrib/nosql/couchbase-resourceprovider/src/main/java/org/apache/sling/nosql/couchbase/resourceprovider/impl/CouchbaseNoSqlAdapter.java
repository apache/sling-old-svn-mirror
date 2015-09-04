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

import java.util.Iterator;

import org.apache.sling.nosql.couchbase.client.CouchbaseClient;
import org.apache.sling.nosql.couchbase.client.CouchbaseKey;
import org.apache.sling.nosql.generic.adapter.AbstractNoSqlAdapter;
import org.apache.sling.nosql.generic.adapter.NoSqlData;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.view.Stale;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewRow;

/**
 * {@link org.apache.sling.nosql.generic.adapter.NoSqlAdapter} implementation for Couchbase.
 */
public final class CouchbaseNoSqlAdapter extends AbstractNoSqlAdapter {

    /**
     * Property holding path
     */
    public static final String PN_PATH = "path";
    
    /**
     * Property holding properties data
     */
    public static final String PN_DATA = "data";

    private static final String VIEW_DESIGN_DOCUMENT = "resourceIndex";
    private static final String VIEW_PARENT_PATH = "parentPath";
    private static final String VIEW_ANCESTOR_PATH = "ancestorPath";

    private final CouchbaseClient couchbaseClient;
    private final String cacheKeyPrefix;

    public CouchbaseNoSqlAdapter(CouchbaseClient couchbaseClient, String cacheKeyPrefix) {
        this.couchbaseClient = couchbaseClient;
        this.cacheKeyPrefix = cacheKeyPrefix;
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
                return new NoSqlData(path, MapConverter.mapListToArray(data.toMap()));
            }
        }
    }

    @Override
    public Iterator<NoSqlData> getChildren(String parentPath) {
        Bucket bucket = couchbaseClient.getBucket();
        // fetch all direct children of this path
        final Iterator<ViewRow> results = bucket.query(
                ViewQuery.from(VIEW_DESIGN_DOCUMENT, VIEW_PARENT_PATH).key(parentPath).stale(Stale.FALSE)).rows();
        return new Iterator<NoSqlData>() {
            @Override
            public boolean hasNext() {
                return results.hasNext();
            }

            @Override
            public NoSqlData next() {
                JsonDocument doc = results.next().document();
                JsonObject envelope = doc.content();
                String path = envelope.getString(PN_PATH);
                JsonObject data = envelope.getObject(PN_DATA);
                return new NoSqlData(path, MapConverter.mapListToArray(data.toMap()));
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
        envelope.put(PN_DATA, JsonObject.from(MapConverter.mapArrayToList(data.getProperties())));

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
        // fetch referenced item and all descendants
        Iterator<ViewRow> results = bucket.query(
                ViewQuery.from(VIEW_DESIGN_DOCUMENT, VIEW_ANCESTOR_PATH).key(path).stale(Stale.FALSE)).rows();
        boolean deletedAny = false;
        while (results.hasNext()) {
            ViewRow result = results.next();
            bucket.remove(result.document());
            deletedAny = true;
        }
        return deletedAny;
    }

}
