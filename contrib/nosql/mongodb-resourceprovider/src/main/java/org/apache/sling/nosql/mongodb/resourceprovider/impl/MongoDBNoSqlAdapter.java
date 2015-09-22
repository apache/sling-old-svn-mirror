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
package org.apache.sling.nosql.mongodb.resourceprovider.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.nosql.generic.adapter.AbstractNoSqlAdapter;
import org.apache.sling.nosql.generic.adapter.MultiValueMode;
import org.apache.sling.nosql.generic.adapter.NoSqlData;
import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * {@link org.apache.sling.nosql.generic.adapter.NoSqlAdapter} implementation for MongoDB.
 */
public final class MongoDBNoSqlAdapter extends AbstractNoSqlAdapter {
    
    private static final String PN_PATH = "_id";
    private static final String PN_PARENT_PATH = "parentPath";
    private static final String PN_DATA = "data";
    
    private final MongoCollection<Document> collection;
    
    /**
     * @param mongoClient MongoDB client
     * @param database MongoDB database
     * @param collection MongoDB collection
     */
    public MongoDBNoSqlAdapter(MongoClient mongoClient, String database, String collection) {
        MongoDatabase db = mongoClient.getDatabase(database);
        this.collection = db.getCollection(collection);
    }

    @Override
    public NoSqlData get(String path) {
        Document envelope = collection.find(Filters.eq(PN_PATH, path)).first();
        if (envelope == null) {
            return null;
        }
        else {
            return new NoSqlData(path, envelope.get(PN_DATA, Document.class), MultiValueMode.LISTS);
        }
    }

    @Override
    public Iterator<NoSqlData> getChildren(String parentPath) {
        List<NoSqlData> children = new ArrayList<>();
        FindIterable<Document> result = collection.find(Filters.eq(PN_PARENT_PATH, parentPath));
        try (MongoCursor<Document> envelopes = result.iterator()) {
            while (envelopes.hasNext()) {
                Document envelope = envelopes.next();
                String path = envelope.get(PN_PATH, String.class);
                Document data = envelope.get(PN_DATA, Document.class);
                children.add(new NoSqlData(path, data, MultiValueMode.LISTS));
            }
        }
        return children.iterator();
    }

    @Override
    public boolean store(NoSqlData data) {
        Document envelope = new Document();
        envelope.put(PN_PATH, data.getPath());
        envelope.put(PN_DATA, new Document(data.getProperties(MultiValueMode.LISTS)));
        
        // for list-children query efficiency store parent path as well
        String parentPath = ResourceUtil.getParent(data.getPath());
        if (parentPath != null) {
            envelope.put(PN_PARENT_PATH, parentPath);
        }
                
        UpdateResult result = collection.replaceOne(Filters.eq(PN_PATH, data.getPath()), envelope, new UpdateOptions().upsert(true));
        
        // return true if a new entry was inserted, false if an existing was replaced
        return (result.getMatchedCount() == 0);
    }

    @Override
    public boolean deleteRecursive(String path) {        
        Pattern descendantsAndSelf = Pattern.compile("^" + Pattern.quote(path) + "(/.+)?$");
        DeleteResult result = collection.deleteMany(Filters.regex(PN_PATH, descendantsAndSelf));
        
        // return true if any document was deleted
        return result.getDeletedCount() > 0;
    }

}
